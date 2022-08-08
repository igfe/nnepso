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

if __name__ == '__main__':
	paths = get_pq_files("out/")
	df = read_pq_as_df(paths[0])
	plot_from_df(df)
