rm results/*
CMD="sbt \"run $@\""
echo $CMD
eval $CMD
# python3 src/python/view.py
