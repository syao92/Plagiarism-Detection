package solutions;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class LSH {
	String databaseFilename;
	int k; 
	int nShingles;
	int nHash;
	int nDocs;
	int seed;
	int [] a;
	int [] b;
	
	int rows; 
	int bands; 
	int lenBuckets;
	double t;
	int [][] signatureMatrix;
	boolean [][] shingleMatrix;
	int [][][] arrayBuckets;
	ArrayList<String> docs = new ArrayList<String>();
	HashMap<String, Integer> shingles = new HashMap<String,Integer>();
	
	public LSH(String databaseFilename, int k, int seed, int nHash, int lenBuckets,
			int bands, int rows, double t) {
		this.databaseFilename = databaseFilename;
		this.k = k;
		this.seed = seed;
		this.nHash = nHash;
		this.lenBuckets = lenBuckets;
		this.bands = bands;
		this.rows = rows;
		this.t = t;
	}
	
	public int computeUniqueShingles() {
		// Read File and add it to the ArrayList for docs. 
        String line; 
        try { 
        	FileReader fileReader = new FileReader(databaseFilename); 
        	BufferedReader buffer = new BufferedReader(fileReader);
	        while ((line = buffer.readLine()) != null) { 
	            // Lowercase, removing special symbols and spaces 
	            line = line.toLowerCase();
	            line = line.replaceAll("\\s", "");
	            line = line.replaceAll("[^a-z0-9]", ""); 
	            docs.add(line);
	        }
	        buffer.close(); 
        } catch (FileNotFoundException e1) { 
        	// TODO Auto-generated catch block 
        	 e1.printStackTrace(); 
        }
        catch (IOException e) { 
            // TODO Auto-generated catch block 
            e.printStackTrace(); 
        } 
		
		nDocs = docs.size();
		int length;
		String sub;
		// Iterate through each line
		for (String s: docs) {
			length = s.length() - k;
			for (int i = 0; i <= length; i++) {
				sub = s.substring(i, i + k);
				if (!shingles.containsKey(sub)) {
					shingles.put(sub, shingles.size());
				} 
			}
		}
		nShingles = shingles.size();
		return nShingles;	
	}
	
	public boolean[][] computeShingleMatrix() {
		String currentDoc;
		String currentShingle = "";
		shingleMatrix = new boolean[nShingles][nDocs];
		
		for (int i = 0; i < nShingles; i++) {
			// Find the correct Shingle to match
			for (String s: shingles.keySet()) {
				if (shingles.get(s) == i) {
					currentShingle = s;
					break;
				}
			}
			
			// Find if the line is contained in doc.
			for (int j = 0; j < nDocs; j++) {
				currentDoc = docs.get(j);
				if (currentDoc.contains(currentShingle)) {
					shingleMatrix[i][j] = true;
				}
			}
		}
		return shingleMatrix;
	}
	
	public int[][] shingleMatrixToSignatureMatrix() {
		signatureMatrix = new int[nHash][nDocs];
		int hashValue;
		
		for (int i = 0; i < nHash; i++) {
			for (int j = 0; j < nDocs; j++) {
				signatureMatrix[i][j] = Integer.MAX_VALUE;
			}
		}
		
		Random generator = new Random(seed);
		a = new int[nHash];
		b = new int[nHash];
		for (int i = 0; i < nHash; i++) {
			a[i] = generator.nextInt(1000) + 1;
			b[i] = generator.nextInt(1000) + 1;
		}
		
		for (int i = 0; i < nShingles; i++) {
			for (int j = 0; j < nDocs; j++) {
				if (shingleMatrix[i][j] == true) {
					for (int k = 0; k < nHash; k++) {
						hashValue = (a[k] * i +b[k]) % nShingles;
						if (signatureMatrix[k][j] > hashValue) {
							signatureMatrix[k][j] = hashValue;
						}
					}
				}
			}
		}
		return signatureMatrix;	
	}
	
	public int[][][] doLSH() {
		arrayBuckets = new int[bands][lenBuckets][100];
		int number = nHash/bands;
		int index = 0;
		int sum = 0;
			
		for (int i = 0; i < bands; i++) {
			for (int j = 0; j < lenBuckets; j++) {
				for (int k = 0; k < 100 ; k++) {
					arrayBuckets[i][j][k] = -1;
				}
			}
		}
		
		for (int i = 0; i < bands; i++) {
			for (int j = 0; j < nDocs; j++) {
				for (int k = 0; k < rows; k++) {
					sum += signatureMatrix[(i * number + k)][j];
				}
				sum = sum % lenBuckets;
				while (arrayBuckets[i][sum][index] != -1) {
					index++;
				}
				arrayBuckets[i][sum][index] = j;
				sum = 0;
				index = 0;
				
			}
		}
		
		return arrayBuckets;	
	}
	
	public int nearestNeighbor(int d) {
		double numerator = 0;
		double denominator = 0;
		double max = 0;
		int index = 0;
		double answer;
		ArrayList<Integer> list = new ArrayList<Integer>(); 
		
		for (int i = 0; i < bands; i++) {
			for (int j = 0; j < lenBuckets; j++) {
				for (int k = 0; k < 100; k++) {
					if (arrayBuckets[i][j][k] == -1) {
						break;
					} else if (arrayBuckets[i][j][k] == d) {
						for (int l = 0; l < 100; l++) {
							if (arrayBuckets[i][j][l] == -1) {
								break;
							} else if (arrayBuckets[i][j][l] != d) {
								list.add(arrayBuckets[i][j][l]);
							}
						}
					}
				}
			}
		}
		
		for (Integer temp: list) {
			for (int i = 0; i < nShingles; i++) {
				if (shingleMatrix[i][d] == true && shingleMatrix[i][temp] == true) {
					numerator++;
					denominator++;
				} else if (shingleMatrix[i][d] == true || shingleMatrix[i][temp] == true) {
					denominator++;
				}
			}
			answer = numerator/denominator;
			if (answer > t && answer > max) {
				index = temp;
				max = answer;
			}
			numerator = 0;
			denominator = 0;
		}
		if (max != 0) {
			return index;
		}
		
		return -1;
		
	}
	
	
	
	
}
