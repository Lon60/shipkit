package cmd

import (
	"os"

	"github.com/spf13/cobra"
)

var completionCmd = &cobra.Command{
	Use:       "completion [bash|zsh|fish]",
	Short:     "Generate shell completion scripts",
	Args:      cobra.MatchAll(cobra.ExactArgs(1), cobra.OnlyValidArgs),
	ValidArgs: []string{"bash", "zsh", "fish"},
	Hidden:    true,
	RunE: func(cmd *cobra.Command, args []string) error {
		switch args[0] {
		case "bash":
			return rootCmd.GenBashCompletionV2(os.Stdout, false)
		case "zsh":
			return rootCmd.GenZshCompletionNoDesc(os.Stdout)
		case "fish":
			return rootCmd.GenFishCompletion(os.Stdout, false)
		}
		return nil
	},
}

func init() {
	rootCmd.AddCommand(completionCmd)
}
