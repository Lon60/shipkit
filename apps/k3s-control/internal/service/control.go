package service

import (
	"context"
	"fmt"
	"os"
	"strings"

	pb "github.com/shipkit/k3s-control/proto"
	v1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/meta"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/serializer/yaml"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/discovery"
	"k8s.io/client-go/discovery/cached/memory"
	"k8s.io/client-go/dynamic"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/restmapper"
	"k8s.io/client-go/tools/clientcmd"
)

// Ensure implementation satisfies interface
var _ pb.K3SControlServiceServer = (*Service)(nil)

// Service implements K3sControlServiceServer backed by the Kubernetes API.
// If Kubernetes config cannot be loaded (e.g., during unit tests), it falls back
// to a noop mode that returns successful ActionResults without touching the
// cluster, allowing tests to run without a cluster.
type Service struct {
	pb.UnimplementedK3SControlServiceServer

	clientset *kubernetes.Clientset
	dynamic   dynamic.Interface
	mapper    meta.RESTMapper
	noop      bool // true when k8s client could not be initialised
}

// New initialises Kubernetes clients. It first tries in-cluster config and then
// falls back to local kubeconfig. If both fail, the service operates in noop
// mode.
func New() *Service {
	cfg, err := rest.InClusterConfig()
	if err != nil {
		// Try kubeconfig from env or default path
		kubeconfig := os.Getenv("KUBECONFIG")
		if kubeconfig == "" {
			kubeconfig = fmt.Sprintf("%s/.kube/config", os.Getenv("HOME"))
		}
		cfg, err = clientcmd.BuildConfigFromFlags("", kubeconfig)
	}

	svc := &Service{}
	if err != nil {
		// No kube config → noop mode
		svc.noop = true
		return svc
	}

	// Increase QPS and Burst for faster apply operations
	cfg.QPS = 50
	cfg.Burst = 100

	clientset, csErr := kubernetes.NewForConfig(cfg)
	dyn, dyErr := dynamic.NewForConfig(cfg)
	disc, dErr := discovery.NewDiscoveryClientForConfig(cfg)
	if csErr != nil || dyErr != nil || dErr != nil {
		svc.noop = true
		return svc
	}

	mapper := restMapper(disc)

	svc.clientset = clientset
	svc.dynamic = dyn
	svc.mapper = mapper
	return svc
}

// restMapper builds a RESTMapper using cached discovery.
func restMapper(dc discovery.DiscoveryInterface) meta.RESTMapper {
	return restmapper.NewShortcutExpander(restmapper.NewDeferredDiscoveryRESTMapper(memory.NewMemCacheClient(dc)), dc)
}

// ApplyDeployment server-side-applies the provided YAML manifest into namespace
// deploy-<uuid>. If the namespace does not exist, it is created.
func (s *Service) ApplyDeployment(ctx context.Context, req *pb.ApplyRequest) (*pb.ActionResult, error) {
	if s.noop {
		return &pb.ActionResult{Status: 0, Message: "noop apply"}, nil
	}

	ns := fmt.Sprintf("deploy-%s", req.Uuid)

	// Ensure namespace exists
	if _, err := s.clientset.CoreV1().Namespaces().Get(ctx, ns, metav1.GetOptions{}); err != nil {
		_, err = s.clientset.CoreV1().Namespaces().Create(ctx, &v1.Namespace{
			ObjectMeta: metav1.ObjectMeta{
				Name: ns,
			}}, metav1.CreateOptions{})
		if err != nil {
			return &pb.ActionResult{Status: 1, Message: "failed to create namespace", Details: err.Error()}, nil
		}
	}

	// Split YAML documents (---) and apply each
	documents := strings.Split(req.ManifestYaml, "---")
	d := yaml.NewDecodingSerializer(unstructured.UnstructuredJSONScheme)

	for _, doc := range documents {
		doc = strings.TrimSpace(doc)
		if doc == "" {
			continue
		}

		obj := &unstructured.Unstructured{}
		_, gvk, err := d.Decode([]byte(doc), nil, obj)
		if err != nil {
			return &pb.ActionResult{Status: 1, Message: "YAML decode error", Details: err.Error()}, nil
		}

		mapping, err := s.mapper.RESTMapping(gvk.GroupKind(), gvk.Version)
		if err != nil {
			if meta.IsNoMatchError(err) {
				if r, ok := s.mapper.(meta.ResettableRESTMapper); ok {
					r.Reset()
					mapping, err = s.mapper.RESTMapping(gvk.GroupKind(), gvk.Version)
				}
			}
			if err != nil {
				return &pb.ActionResult{Status: 1, Message: "REST mapping error", Details: err.Error()}, nil
			}
		}

		// Namespaced?
		var ri dynamic.ResourceInterface
		if mapping.Scope.Name() == meta.RESTScopeNameNamespace {
			if obj.GetNamespace() == "" {
				obj.SetNamespace(ns)
			}
			ri = s.dynamic.Resource(mapping.Resource).Namespace(obj.GetNamespace())
		} else {
			ri = s.dynamic.Resource(mapping.Resource)
		}

		obj.SetManagedFields(nil) // remove managed fields if any
		data, err := runtime.Encode(unstructured.UnstructuredJSONScheme, obj)
		if err != nil {
			return &pb.ActionResult{Status: 1, Message: "encode error", Details: err.Error()}, nil
		}

		fieldManager := "k3s-control"
		_, err = ri.Patch(ctx, obj.GetName(), types.ApplyPatchType, data, metav1.PatchOptions{FieldManager: fieldManager, Force: ptrBool(true)})
		if err != nil {
			return &pb.ActionResult{Status: 1, Message: "apply error", Details: err.Error()}, nil
		}
	}

	return &pb.ActionResult{Status: 0, Message: "applied successfully"}, nil
}

func ptrBool(b bool) *bool {
	return &b
}

// DeleteDeployment deletes the namespace deploy-<uuid>.
func (s *Service) DeleteDeployment(ctx context.Context, req *pb.DeleteRequest) (*pb.ActionResult, error) {
	if s.noop {
		return &pb.ActionResult{Status: 0, Message: "noop delete"}, nil
	}

	ns := fmt.Sprintf("deploy-%s", req.Uuid)
	propagation := metav1.DeletePropagationBackground
	err := s.clientset.CoreV1().Namespaces().Delete(ctx, ns, metav1.DeleteOptions{PropagationPolicy: &propagation})
	if err != nil && !apierrors.IsNotFound(err) {
		return &pb.ActionResult{Status: 1, Message: "delete error", Details: err.Error()}, nil
	}
	return &pb.ActionResult{Status: 0, Message: "delete requested"}, nil
}

func (s *Service) GetStatus(ctx context.Context, req *pb.StatusRequest) (*pb.AppStatus, error) {
	if s.noop {
		return &pb.AppStatus{Uuid: req.Uuid, Status: 0, Message: "noop status", State: pb.AppState_UNKNOWN}, nil
	}

	ns := fmt.Sprintf("deploy-%s", req.Uuid)

	namespaceObj, errNs := s.clientset.CoreV1().Namespaces().Get(ctx, ns, metav1.GetOptions{})
	if errNs != nil {
		if apierrors.IsNotFound(errNs) {
			return &pb.AppStatus{Uuid: req.Uuid, Status: 0, Message: "namespace deleted", State: pb.AppState_STOPPED}, nil
		}
		return &pb.AppStatus{Uuid: req.Uuid, Status: 1, Message: errNs.Error(), State: pb.AppState_ERROR}, nil
	}

	if namespaceObj.DeletionTimestamp != nil {
		return &pb.AppStatus{Uuid: req.Uuid, Status: 0, Message: "namespace terminating", State: pb.AppState_STOPPING}, nil
	}

	pods, err := s.clientset.CoreV1().Pods(ns).List(ctx, metav1.ListOptions{})
	if err != nil {
		if apierrors.IsNotFound(err) {
			// Namespace no longer exists → deployment is stopped
			return &pb.AppStatus{Uuid: req.Uuid, Status: 0, Message: "namespace deleted", State: pb.AppState_STOPPED}, nil
		}
		return &pb.AppStatus{Uuid: req.Uuid, Status: 1, Message: err.Error(), State: pb.AppState_ERROR}, nil
	}

	if len(pods.Items) == 0 {
		return &pb.AppStatus{Uuid: req.Uuid, Status: 0, Message: "no pods", State: pb.AppState_STOPPED}, nil
	}

	var containerStatuses []*pb.ContainerStatus
	overallState := pb.AppState_RUNNING

	for _, p := range pods.Items {
		for _, cs := range p.Status.ContainerStatuses {
			cstate := "unknown"
			if cs.State.Running != nil {
				if p.ObjectMeta.DeletionTimestamp != nil {
					cstate = "terminating"
					overallState = pb.AppState_STOPPING
				} else {
					cstate = "running"
				}
			} else if cs.State.Waiting != nil {
				cstate = "waiting"
				overallState = pb.AppState_STARTING
			} else if cs.State.Terminated != nil {
				cstate = "terminated"
				overallState = pb.AppState_ERROR
			}

			readiness := "notReady"
			if cs.Ready {
				readiness = "ready"
			}

			// collect ports from pod spec containers
			var ports []string
			for _, c := range p.Spec.Containers {
				if c.Name == cs.Name {
					for _, port := range c.Ports {
						proto := strings.ToUpper(string(port.Protocol))
						ports = append(ports, fmt.Sprintf("%d/%s", port.ContainerPort, proto))
					}
				}
			}

			containerStatuses = append(containerStatuses, &pb.ContainerStatus{
				Name:      cs.Name,
				State:     cstate,
				Readiness: readiness,
				Ports:     ports,
			})
		}
	}

	return &pb.AppStatus{Uuid: req.Uuid, Status: 0, Message: "ok", State: overallState, Containers: containerStatuses}, nil
}
