package edu.asu.ca.kaushik.algorithms.derandomized.pe.structures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;

import edu.asu.ca.kaushik.algorithms.structures.ColGroup;
import edu.asu.ca.kaushik.algorithms.structures.Helper;
import edu.asu.ca.kaushik.algorithms.structures.Interaction;
import edu.asu.ca.kaushik.algorithms.structures.SymTuple;

public class InteractionSet {
	private int t;
	private int k;
	private int v;
	private Map<ColGroup, Set<IFitness>> iSet;
	int[][] deletionEsitmate; 
	Interaction bInteraction;

	public InteractionSet(int t, int k, int v) {
		this.t = t;
		this.k = k;
		this.v = v;
		
		this.iSet = createFullInteractionSet(this.t, this.k, this.v);
		this.deletionEsitmate = new int[this.v][this.k];
		this.bInteraction = this.findBestInteraction(0);
	}

	private Map<ColGroup, Set<IFitness>> createFullInteractionSet(int t,
			int k, int v) {
		List<ColGroup> allColGroups = Helper.createAllColGroups(t, k);
		List<SymTuple> symTuples = Helper.createAllSymTuples(t, v);
		
		Map<ColGroup, Set<IFitness>> iSet = new HashMap<ColGroup, Set<IFitness>>();
		
		for (ColGroup cols : allColGroups) {
			Set<IFitness> iFitnessSet = this.createIFitnessSet(symTuples, t, k, v);
			iSet.put(cols, iFitnessSet);
		}
		
		return iSet;
	}

	private Set<IFitness> createIFitnessSet(List<SymTuple> symTuples, int t, int k, int v) {
		Set<IFitness> iFitnessSet = new HashSet<IFitness>();
		for (SymTuple symTuple : symTuples) {
			iFitnessSet.add(new IFitness(symTuple, t, k, v));
		}
		return iFitnessSet;
	}

	public boolean isEmpty() {
		return this.iSet.isEmpty();
	}

	public int deleteInteractions(Integer[] newRow) {
		int coverage = 0;
		this.resetDeletionEstimates();
		
		Iterator<ColGroup>	colGrIt = this.iSet.keySet().iterator();
		while (colGrIt.hasNext()) {
			ColGroup colGr = colGrIt.next();
			int[] indices = colGr.getCols();
			
			int[] syms = new int[this.t];
			for (int i = 0; i < this.t; i++) {
				syms[i] = newRow[indices[i]].intValue();
			}
			SymTuple tuple = new SymTuple(syms);
			
			Set<IFitness> iFitnessSet = this.iSet.get(colGr);
			if (iFitnessSet.contains(tuple)) {
				coverage += 1;
				this.updateDeletionEstimate(colGr, tuple);
				iFitnessSet.remove(tuple);
				if (iFitnessSet.isEmpty()) {
					colGrIt.remove();
				}
			}
		}
		
		this.bInteraction = this.findBestInteraction(coverage);
		
		return coverage;
	}

	private void resetDeletionEstimates() {
		for (int i = 0; i < this.v; i++) {
			for (int j = 0; j < this.k; j++) {
				this.deletionEsitmate[i][j] = 0;
			}
		}	
	}
	
	private void updateDeletionEstimate(ColGroup colGr, SymTuple tuple) {
		int[] syms = tuple.getSyms();
		int[] cols = colGr.getCols();
		int t = syms.length;
		for (int i = 0; i < t; i++) {
			this.deletionEsitmate[syms[i]][cols[i]]++;
		}		
	}
	
	private Interaction findBestInteraction(int coverage) {
		double maxEstExpCoverage =  0.0d;
		ColGroup bColGr = null;
		SymTuple bSymTup =  null;
		
		Iterator<ColGroup>	colGrIt = this.iSet.keySet().iterator();
		while (colGrIt.hasNext()) {
			ColGroup colGr = colGrIt.next();
			
			Iterator<IFitness> iFitnessSetIt = this.iSet.get(colGr).iterator();
			if (iFitnessSetIt.hasNext()) {
				IFitness iFitness = iFitnessSetIt.next();
				this.updateOverlapCount(colGr, iFitness, coverage);
				double estExpCoverage = this.estimateExpCoverage(iFitness);
				if (estExpCoverage > maxEstExpCoverage){
					maxEstExpCoverage = estExpCoverage;
					bColGr = colGr;
					bSymTup = iFitness.getSymTup();
					
				} 
			}
		}
		
		return new Interaction(bColGr, bSymTup);
	}

	private void updateOverlapCount(ColGroup colGr, IFitness iFitness, int coverage) {
		long[] overlapC = iFitness.getOverlapCountA();
		int[] syms = iFitness.getSymTup().getSyms();
		int[] cols = colGr.getCols();
		int t = cols.length;
		int[] decrement = new int[t];
		int sum = 0;
		for (int i = 1; i < t; i++) {
			List<ColGroup> overlapSet = Helper.createAllColGroups(i, t);
			decrement[i] = 0;
			for (ColGroup s : overlapSet) {
				decrement[i] = decrement[i] + this.estimatedDeletions(s.getCols(), cols, syms);
			}
			overlapC[i] = overlapC[i] - decrement[i];
			sum = sum + decrement[i];
		}
		
		decrement[0] = coverage - sum;
		if (decrement[0] > 0) {
			overlapC[0] = overlapC[0] - decrement[0];	
		}
		
	}

	private int estimatedDeletions(int[] s, int[] cols, int[] syms) {
		int i = s.length;
		int min = Integer.MAX_VALUE;
		for (int j = 0; j < i; j++) {
			if (this.deletionEsitmate[syms[s[j]]][cols[s[j]]] < min) {
				min = this.deletionEsitmate[syms[s[j]]][cols[s[j]]];
			}
		}
		return min;
	}
	
	private double estimateExpCoverage(IFitness iFitness) {
		long[] overlapC = iFitness.getOverlapCountA();
		double estExpCov = 0.0d; 
		for (int i = 0; i < this.t; i++) {
			estExpCov = estExpCov + (overlapC[i] * this.computeProb(i));
		}
		return estExpCov;
	}

	private double computeProb(int i) {
		return 1.0d / FastMath.pow(this.v, this.t - i);
	}

	public Interaction selectBestInteraction(Integer[] newRow) {
		return this.bInteraction;
	}

	public int gett() {
		return this.t;
	}

	public int getk() {
		return this.k;
	}

	public boolean contains(Interaction interaction) {
		ColGroup colGr = interaction.getCols();
		SymTuple tuple = interaction.getSyms();
		
		if (this.iSet.containsKey(colGr)){
			Set<IFitness> iFitnessSet = this.iSet.get(colGr);
			if (iFitnessSet.contains(tuple)){
				return true;
			}
		}
		return false;
	}
}
