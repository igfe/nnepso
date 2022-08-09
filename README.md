# nnepso
MSC thesis related code

## dependencies
- [cilib](https://github.com/ciren/cilib) provides reproducable and type-safe PSO execution.
- [zio](https://github.com/zio/zio) is a concurrency library and second order dependency of cilib. It is also used for the [CLI](https://github.com/zio/zio-cli) of this project.
- [nix](https://nixos.org/) is used for system level dependencies.
- [parquet](https://parquet.apache.org/) file format is used for performance graphs output.

If output isn't cleaned manually, the program will fail to run. Use `sbt clean` to remove output files.
Output is written in the `.parquet` format. This can be read and formatted using the `pqrs` command line utility
Alternatively, results are interpreted using `python3 src/python/view.py` which is reliant on `pyarrow` to read the `.parquet` format

## execution
The dependencies automatically handled by the build tools `nix` and `sbt`. After [installing nix](https://nixos.org/download.html):
- run `nix-shell` from the root directory
- enter the sbt REPL by `sbt` 
- use `run <pso> -p <problem>` and the problem parameters.

## development
I use Sublime Text with [LSP](https://packagecontrol.io/packages/LSP) and [LSP-metals](https://packagecontrol.io/packages/LSP-metals). Sublime text native `reindent` breaks when it encounters some scala syntax, I use `LSP: Format File` from the command palette or the `ctrl+alt+l` shortcut.