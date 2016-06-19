# neovim-intellij-complete
IntelliJ completion in [Neovim](https://neovim.io/). Depends on [neovim-java-client](https://github.com/vhakulinen/neovim-java-client).

# Install It

Download the zip file from [releases page](https://github.com/vhakulinen/neovim-intellij-complete/releases).
This is the IntelliJ plugin which provides functionality for Neovim which you can
use with deoplete and [neovim-intellij-complete-deoplete](https://github.com/vhakulinen/neovim-intellij-complete-deoplete).

Install the neovim-intellij-complete plugin for IDEA, Android Studio, or 
other Jetbrains IDE and deoplete + neovim-intellij-complete-deoplete for Neovim.

# Use It
* Run `NVIM_LISTEN_ADDRESS=127.0.0.1:7650 nvim` 
* Select `neovim->connect` from your IDE, from the top bar (which should be there if you've installed the
IntelliJ plugin correctly).
* Enter `127.0.0.1:7650` in the dialog box that opens. 
* Your Neovim instance should echo `Intellij connected`.

Now the autocompletion should work. Note that it will only work files which are 
included in the project which is opened on your IDE which you connected to neovim.
