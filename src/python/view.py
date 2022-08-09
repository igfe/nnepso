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

def read_pq_as_df(path):
	print("reading parquet table")
	table = pq.read_table(path)
	print("parquet to pandas")
	df = table.to_pandas()
	return df

def df_preprocess(df):
	#flatten data column from pq
	df = df.join(pd.DataFrame(df.pop('data').values.tolist())) 
	# remove columns with redundant data
	data = dict()
	reducable_cols = ["algorithm", "problem", "env", "seed"]
	for c in reducable_cols: 
		u = df[c].unique()
		if len(u) == 1:
			data[c] = u[0]
		else:
			data[c] = u
	df = df.drop(reducable_cols, axis=1)
	# take means from independent runs
	df = df.groupby("iteration").mean() # sorts by iteration too, maybe pass x values to plot
	return data, df

def plot_from_df(data, df):
	plot_column(df, "min")
	plot_column(df, "average")

	title = f'{data["problem"]} with {data["algorithm"]}'
	print("plotting")
	plt.title(title)
	plt.xlabel("iteration")
	plt.ylabel("fitness")
	plt.yscale('symlog')
	plt.legend()
	plt.show()

def plot_column(df, column):
	print(f"with {column=}")
	xs = df[column]
	# xs = df["iteration"]
	plt.plot(xs, label=column)

def selector(paths):
	c = 0
	if len(paths) > 1:
		print("too many .parquet files, select one")
		for i in range(len(paths)):
			print(f"[{i}]\t{paths[i]}")
		c = int(input())
		if c > len(paths) - 1:
			print("invalid input")
			pass
	path = paths[c]
	print(f"selected: {path}")
	df = read_pq_as_df(path)
	data, df = df_preprocess(df)
	plot_from_df(data, df)

def main():
	paths = get_pq_files("out/")
	selector(paths)

if __name__ == '__main__':
	main()
