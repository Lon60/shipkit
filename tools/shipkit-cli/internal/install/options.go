package install

// Overlay represents which kustomize overlay to apply.
type Overlay string

const (
	OverlayDevelopment Overlay = "development"
	OverlayProduction  Overlay = "production"
)

// InstallOptions contains parameters that control installation behavior.
type InstallOptions struct {
	Overlay Overlay
	// Paths are expected to be absolute
	BaseValuesPath   string
	DevTLSValuesPath string // only used when UseMkcert is true
	OverlayPath      string

	// Env file for ConfigMap; if empty, defaults are used
	EnvFilePath string

	// When true, attempt to use mkcert-generated TLS secret and dev TLS values
	UseMkcert bool

	// Chart versions and other tunables
	TraefikChartVersion string // e.g., v37.0.0
}
