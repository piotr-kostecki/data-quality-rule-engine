-- Project-local Neovim config for LazyVim. lazy.nvim loads this file after the user config, and asks
-- once to trust it (:trust). Start Neovim from inside the Nix/direnv shell so JAVA_HOME is the
-- project's JDK 21.
--
-- Formatting rules live in editor-neutral files, so every editor formats the same way:
--   .editorconfig                 charset, line endings, indentation, 120 columns (all files)
--   config/eclipse-formatter.xml  Java formatter profile, applied here through jdtls

local root = vim.fs.root(vim.uv.cwd(), ".lazy.lua") or vim.uv.cwd()

vim.api.nvim_create_autocmd("FileType", {
  group = vim.api.nvim_create_augroup("project_local", { clear = true }),
  pattern = { "java", "groovy", "markdown" },
  callback = function(ev)
    if ev.match == "markdown" then
      -- Prettier would realign the wide tables in DESIGN.md on every save
      vim.b[ev.buf].autoformat = false
      return
    end
    -- .editorconfig sets textwidth=120: show the limit, but don't hard-wrap code while typing
    vim.opt_local.colorcolumn = "+1"
    vim.opt_local.formatoptions:remove("t")
  end,
})

return {
  {
    "mfussenegger/nvim-jdtls",
    opts = {
      settings = {
        java = {
          format = {
            enabled = true,
            settings = { url = root .. "/config/eclipse-formatter.xml", profile = "project" },
          },
          -- Imports as in the code base: other packages, then javax.*, then java.*, then static imports.
          -- No wildcard imports.
          completion = { importOrder = { "", "javax", "java", "#" } },
          sources = { organizeImports = { starThreshold = 999, staticStarThreshold = 999 } },
          -- JSpecify @NullMarked / @Nullable are understood by the null analysis
          compile = { nullAnalysis = { mode = "automatic" } },
          configuration = { updateBuildConfiguration = "automatic" },
          import = {
            gradle = {
              enabled = true,
              wrapper = { enabled = true },
              -- Gradle 8.14 can't run on JDK 25+, so import with the project's JDK when it is known
              java = { home = vim.env.JAVA_HOME },
            },
          },
        },
      },
    },
  },
}
