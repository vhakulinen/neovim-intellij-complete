# neovim-intellij-complete
Intellij completion in neovim. Depends on https://github.com/vhakulinen/neovim-java-client

# Installation

Download the zip file from https://github.com/vhakulinen/neovim-intellij-complete/releases.
This is the intellij plugin which provides functionality for neovim which you can
use with deoplete and https://github.com/vhakulinen/neovim-intellij-complete-deoplete.

Install the neovim-intellij-complete plugin for IDEA or Android Studio and
deoplete + neovim-intellij-complete-deoplete for neovim.

Then run `NVIM_LISTEN_ADDRESS=127.0.0.1:7650 nvim` and from your IDE, select
neovim->connect from the top bar (which should be there if you've installed the
intellij plugin correctly) and entter `127.0.0.1:7650`. Your neovim instannce
should now have echoed `Intellij connected`. Now the autocompletion should work.
Note that it will only work files which are included in the project which is
opened on your IDE which you connected to neovim.
