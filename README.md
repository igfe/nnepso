# nnepso
MSC thesis related code

## dependencies
Most dependencies are handled by `nix` and `sbt`. After installing `nix`, run `nix-shell` from the root directory and execute by using `sbt run` or alternatively `bash run.sh` to automatically clean output. If output isn't cleared manually, the program might fail to run.
Output is written in the `.parquet` format. This can be read and formatted using the `pqrs` command line utility
Alternatively, results are interpreted using `python3 src/python/view.py` which is reliant on `pyarrow` to read the `.parquet` format

## development
I develop mostly in the Sublime Text editor, due to my fear of IDE dotfiles and inconsistent behavior of IDE built in terminals. To help with code formatting and type inference, I use `LSP-metals`. Sublime text native `reindent` breaks when it encounters Scala's pattern matching syntax, so I use `LSP`'s format which can be used from the command palette or `ctrl+alt+l`