package cmd

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
)

var rootCmd = &cobra.Command{
	Use:   "skdev",
	Short: "Shipkit local development CLI",
	Long:  "skdev helps you start/stop the local kind (Podman)-based Shipkit dev environment and run services locally.",
	Run: func(cmd *cobra.Command, args []string) {
		_ = cmd.Help()
	},
}

func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
