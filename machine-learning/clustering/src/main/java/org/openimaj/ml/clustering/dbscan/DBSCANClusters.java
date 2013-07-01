package org.openimaj.ml.clustering.dbscan;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.ml.clustering.assignment.HardAssigner;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class DBSCANClusters<T> implements SpatialClusters<T> {
	/**
	 * The members of a cluster
	 */
	int[][] clusterMembers;
	/**
	 * Indexes of noise elements
	 */
	int[] noise;
	/**
	 * The configuration that created this DBSCAN cluster
	 */
	DBSCANConfiguration<DoubleNearestNeighbours, double[]> conf;

	@Override
	public void readASCII(Scanner in) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String asciiHeader() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void readBinary(DataInput in) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] binaryHeader() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeASCII(PrintWriter out) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeBinary(DataOutput out) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int numDimensions() {
		return 0;
	}

	@Override
	public int numClusters() {
		return this.clusterMembers.length;
	}

	@Override
	public HardAssigner<T, ?, ?> defaultHardAssigner() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		int[][] clusters = clusterMembers;
		int i = 0;
		String str = "";
		for (int[] member : clusters) {
			str += String.format("%d %s\n",i++,Arrays.toString(member));
		}
		str+=String.format("%s", Arrays.toString(this.noise));
		return str;
	}
}