import pyarrow.parquet as pq
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

"""
	TODO:
		add titles and labels to the graphs from parquet entries
		separate extract_data from plot_column 
		use pq or pd to merge data instead of looping over iterations
"""
def get_pq_files(directory):
	extension = ".parquet"
	files = [f for f in os.listdir(directory) if f.endswith(extension)]
	paths = [os.path.join(directory, f) for f in files]
	print(paths)
	return paths

def plot_column(df, column):
	print(f"with {column=}")
	xs = []
	for i in df["iteration"].unique():
		v = [z[column] for z in df.loc[df["iteration"]==i]["data"]]
		xs += [sum(v)/len(v)]
	plt.plot(xs, label=column)

def read_pq_as_df(path):
	print("reading parquet table")
	table = pq.read_table(path)
	print("parquet to pandas")
	df = table.to_pandas()
	return df

def plot_from_df(df):
	plot_column(df, "min")
	plot_column(df, "average")

	print("plotting")
	plt.yscale('symlog')
	plt.legend()
	plt.show()

def selector(paths):
	c = 0
	if len(paths) > 1:
		print("too many .parquet files, select one")
		for i in range(len(paths)):
			print(f"[{i}]\t{paths[i]}")
		cv = int(input())
		if cv > len(paths) - 1:
			print("invalid input, terminating")
			pass
		else:
			df = read_pq_as_df(paths[c])
			plot_from_df(df)

if __name__ == '__main__':
	paths = get_pq_files("out/")
	selector(paths)