import pyarrow.parquet as pq
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

def plot_from_df(df, column):
	print(f"with {column=}")
	xs = []
	for i in df["iteration"].unique():
		v = [z[column] for z in df.loc[df["iteration"]==i]["data"]]
		xs += [sum(v)/len(v)]
	plt.plot(xs, label=column)

print("reading parquet table")
table = pq.read_table('out/results.parquet')
print("parquet to pandas")
df = table.to_pandas()

plot_from_df(df, "min")
plot_from_df(df, "average")

print("plotting")
plt.yscale('symlog')
plt.legend()
plt.show()