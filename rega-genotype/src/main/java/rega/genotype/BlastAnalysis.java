/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.utils.StreamReaderRuntime;

/**
 * Implements similarity based analyses using NCBI blast.
 * 
 * @author koen
 */
public class BlastAnalysis extends AbstractAnalysis {
	public static String blastPath = "";
    public static String formatDbCommand = "formatdb";
    public static String blastCommand = "blastall";

    /**
     * Defines a region in a reference sequence.
     */
    public static class Region {
    	private String name;
    	private int begin, end;
    	
    	public Region(String name, int begin, int end) {
    		this.name = name;
    		this.begin = begin;
    		this.end = end;
    	}

		public int getBegin() {
			return begin;
		}

		public int getEnd() {
			return end;
		}

		public String getName() {
			return name;
		}

		/**
		 * @param queryBegin
		 * @param queryEnd
		 * @param minimumOverlap
		 * @return whether the region overlaps with a region defined by queryBegin
		 *   and queryEnd, with a minimum overlap.
		 */
		public boolean overlaps(int queryBegin, int queryEnd, int minimumOverlap) {
			int overlapBegin = Math.max(queryBegin, begin);
			int overlapEnd = Math.min(queryEnd, end);
			
			return (overlapEnd - overlapBegin) > minimumOverlap;
		}
    }

    public static class ReferenceTaxus {
    	private String taxus;
    	private List<Region> regions;
		private String reportOther;
		private int reportOtherOffset;
		private int priority; // lower value means higher priority

    	public ReferenceTaxus(String taxus, int priority) {
    		this.taxus = taxus;
    		this.priority = priority;
    		this.regions = new ArrayList<Region>();
    	}
    	
    	void setReportAsOther(String taxus, int offset) {
    		this.reportOther = taxus;
    		this.reportOtherOffset = offset;
    	}

    	void addRegion(Region r) {
    		if (this.regions == null)
    			this.regions = new ArrayList<Region>();
    		
    		regions.add(r);
    	}
    	
    	public List<Region> getRegions() {
    		return regions;
    	}

		public String getTaxus() {
			return taxus;
		}
		
		public int getPriority() {
			return priority;
		}

		public String reportAsOther() {
			return reportOther;
		}
		
		public int reportAsOtherOffset() {
			return reportOtherOffset;
		}
    }
    
    private List<Cluster> clusters;
    private Double cutoff;
	private Double maxPValue;
    private boolean relativeCutoff;
    private String blastOptions;
	private String formatDbOptions;
	private Map<String, ReferenceTaxus> referenceTaxa;

	/**
	 * A result from a blast analysis.
	 * 
	 * It contains information on the location of the sequence with respect to a
	 * reference genome (which may be the best match or a predefined referenceTaxus).
	 * 
	 * It also tells you whether the query sequence is reference complimented with
	 * respect to the reference sequences.
	 */
    public class Result extends AbstractAnalysis.Result implements Concludable {
        private Cluster cluster;
        private float score;
        private int start;
        private int end;
        private ReferenceTaxus refseq;
		private boolean reverseCompliment;

        public Result(AbstractSequence sequence, Cluster cluster, float score, int start, int end, ReferenceTaxus refseq, boolean reverseCompliment) {
            super(sequence);
            this.cluster = cluster;
            this.score = score;
            this.start = start;
            this.end = end;
            this.refseq = refseq;
            this.reverseCompliment = reverseCompliment;
        }

        public boolean haveSupport() {
            if (cutoff == null)
                return false;
            else
                return score >= cutoff;
        }
        
        public void writeXML(ResultTracer tracer) {
            writeXMLBegin(tracer);
            if (start > 0) {
            	tracer.add("start", String.valueOf(start));           
            	tracer.add("end", String.valueOf(end));
            }
            tracer.printlnOpen("<cluster>");
            tracer.add("id", cluster != null ? cluster.getId() : "none");
            tracer.add("name", cluster != null ? cluster.getName() : "none");
            tracer.add("score", score);
            if (cluster != null && cluster.getDescription() != null) {
                tracer.add("description", cluster.getDescription());
            }
            tracer.add("reverse-compliment", String.valueOf(reverseCompliment));
            tracer.add("concluded-id", haveSupport() ? cluster.getId() : "Unassigned");
            tracer.add("concluded-name", haveSupport() ? cluster.getName() : "Unassigned");
            tracer.printlnClose("</cluster>");
            if (refseq != null)
            	tracer.add("refseq", refseq.getTaxus());
            writeXMLEnd(tracer);
        }

        /**
         * @return Returns the cluster.
         */
        public Cluster getCluster() {
            return cluster;
        }

        /**
         * @return Returns the score.
         */
        public float getScore() {
            return score;
        }
        
        /**
        * @return Returns the start.
        */
       public int getStart() {
           return start;
       }

       /**
        * @return Returns the end.
        */
       public int getEnd() {
           return end;
       }

        public void writeConclusion(ResultTracer tracer) {
            tracer.printlnOpen("<assigned>");
            tracer.add("id", cluster.getId());
            tracer.add("name", cluster.getName());
            tracer.add("score", String.valueOf(score));
            if (cluster.getDescription() != null) {
                tracer.add("description", cluster.getDescription());
            }
            tracer.printlnClose("</assigned>");
        }

		public Cluster getConcludedCluster() {
			return cluster;
		}

		public float getConcludedSupport() {
			return 0;
		}

		public boolean isReverseCompliment() {
			return reverseCompliment;
		}
		
		public ReferenceTaxus getReference() {
			return refseq;
		}
    }
    
    @Override
	public Result run(AbstractSequence sequence) throws AnalysisException {
		return (rega.genotype.BlastAnalysis.Result) super.run(sequence);
	}

	public BlastAnalysis(AlignmentAnalyses owner, String id,
                         List<Cluster> clusters, Double cutoff, Double maxPValue,
                         boolean relativeCutoff, String blastOptions,
                         File workingDir) {
        super(owner, id);
        this.workingDir = workingDir;
        this.clusters = clusters;
        this.cutoff = cutoff;
        this.maxPValue = maxPValue;
        this.relativeCutoff = relativeCutoff;
        this.blastOptions = blastOptions != null ? blastOptions : "";
        if (owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_AA) {
        	this.blastOptions = "-p blastx";
        	this.formatDbOptions = "";
        } else {
        	this.blastOptions = "-p blastn " + this.blastOptions;
        	this.formatDbOptions = "-p F";
        }
        this.referenceTaxa = new HashMap<String, ReferenceTaxus>();
    }

    private Result compute(SequenceAlignment analysisDb, AbstractSequence sequence)
            throws ApplicationException {
        Process formatdb = null;
        Process blast = null;
        try {
            if (sequence.getLength() != 0) {
                File db = getTempFile("db.fasta");
                FileOutputStream dbFile = new FileOutputStream(db);
                //FileDescriptor fd = dbFile.getFD();
                analysisDb.writeFastaOutput(dbFile);
                //dbFile.flush();
                //fd.sync();
                dbFile.close();

                File query = getTempFile("query.fasta");
                FileOutputStream queryFile = new FileOutputStream(query);
                //FileDescriptor fd2 = dbFile.getFD();
                sequence.writeFastaOutput(queryFile);
                //queryFile.flush();
                //fd2.sync();
                queryFile.close();
                        
                String cmd = blastPath + formatDbCommand + " " + formatDbOptions + " -o T -i " + db.getAbsolutePath();
                System.err.println(cmd);
                
                formatdb = StreamReaderRuntime.exec(cmd, null, workingDir);
                int result = formatdb.waitFor();

                if (result != 0) {
                    throw new ApplicationException("formatdb exited with error: " + result);
                }
                
                cmd = blastPath + blastCommand + " " + blastOptions
                	+ " -i " + query.getAbsolutePath()
                    + " -m 8 -d " + db.getAbsolutePath();
                System.err.println(cmd);

                blast = Runtime.getRuntime().exec(cmd, null, workingDir);
                InputStream inputStream = blast.getInputStream();

                LineNumberReader reader
                    = new LineNumberReader(new InputStreamReader(inputStream));

                int queryFactor = (owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_AA)
                	? 3 : 1;

                String[] best = null, secondBest = null;
                int start = Integer.MAX_VALUE;
                int end = -1;

                boolean reverseCompliment = false;

                ReferenceTaxus refseq = null;
                Cluster bestCluster = null, secondBestCluster = null;

                for (;;) {
                    String s = reader.readLine();
                    if (s == null)
                        break;
                    System.err.println(s);

                    String[] values = s.split("\t");
                    if (values.length != 12)
                    	throw new ApplicationException("blast result format error");

                    if (best == null) {
                    	best = values;
                    	bestCluster = findCluster(best[1]);
                    }

                    ReferenceTaxus referenceTaxus = referenceTaxa.get(values[1]);

                    /*
                     * First condition: there are no explicit reference taxa configured -- use best match
                     * 
                     * Second condition:
                     *  - the referenceTaxus is the first
                     *  - or has a higher priority than the current refseq and belongs to the same cluster 
                     *   (note priority is smaller number means higher priority)
                     */
                    if ((referenceTaxa.isEmpty() && values == best)
                    	|| (referenceTaxus != null
                    		&& (findCluster(values[1]) == bestCluster)
                    		&& (refseq == null || referenceTaxus.getPriority() < refseq.getPriority()))) {
                    	refseq = referenceTaxus;
                    	boolean queryReverseCompliment = Integer.parseInt(values[7]) - Integer.parseInt(values[6]) < 0;
                    	boolean refReverseCompliment = Integer.parseInt(values[9]) - Integer.parseInt(values[8]) < 0;
                    	int offsetBegin = Integer.parseInt(values[6]);
                    	int offsetEnd = sequence.getLength() - Integer.parseInt(values[7]);
                    	if (queryReverseCompliment) {
                    		offsetBegin = sequence.getLength() - offsetBegin;
                    		offsetEnd = sequence.getLength() - offsetEnd;
                    		reverseCompliment = true;
                    	}
                    	if (refReverseCompliment) {
                    		String tmp = values[8];
                    		values[8] = values[9];
                    		values[9] = tmp;
                    		reverseCompliment = true;
                    	}
                    	start = Integer.parseInt(values[8])*queryFactor - offsetBegin;
                    	end = Integer.parseInt(values[9])*queryFactor + offsetEnd;
                    	
                    	if (refseq != null && refseq.reportAsOther() != null) {
                    		refseq = referenceTaxa.get(refseq.reportAsOther());
                    		start += refseq.reportAsOtherOffset();
                    		end += refseq.reportAsOtherOffset();
                    	}
                    }

                    if (relativeCutoff && bestCluster != null && secondBestCluster == null) {
                    	Cluster c = findCluster(values[1]);
                    	if (c != bestCluster) {
                    		secondBestCluster = c;
                    		secondBest = values;
                    	}
                    }
                }
                result = blast.waitFor();

                blast.getErrorStream().close();
                blast.getInputStream().close();
                blast.getOutputStream().close();

                if (result != 0) {
                    throw new ApplicationException("blast exited with error: " + result);
                }

                db.delete();                
                query.delete();

                if (owner.getAlignment().getSequenceType() == SequenceAlignment.SEQUENCE_DNA) {
                    getTempFile("db.fasta.nhr").delete();
                    getTempFile("db.fasta.nin").delete();
                    getTempFile("db.fasta.nsd").delete();
                    getTempFile("db.fasta.nsi").delete();
                    getTempFile("db.fasta.nsq").delete();
                } else {
                    getTempFile("db.fasta.phr").delete();
                    getTempFile("db.fasta.pin").delete();
                    getTempFile("db.fasta.psd").delete();
                    getTempFile("db.fasta.psi").delete();
                    getTempFile("db.fasta.psq").delete();
                }

                if (best != null) {
                    float score = Float.valueOf(best[11]);
                	float pValue = Float.valueOf(best[10]);
                    if (maxPValue != null && pValue > maxPValue)
                    	score = -1;

                    if (relativeCutoff) {
                   		if (secondBest != null)
                   			score = score / Float.valueOf(secondBest[11]);
                    }

                    if (start == Integer.MAX_VALUE)
                    	start = -1;
                    
                    return createResult(sequence, bestCluster, refseq, score, start, end, reverseCompliment);
                } else
                	return createResult(sequence, null, null, 0, 0, 0, false);
            } else
                return createResult(sequence, null, null, 0, 0, 0, false);
        } catch (IOException e) {
            if (formatdb != null)
                formatdb.destroy();
            if (blast != null)
                blast.destroy();
            throw new ApplicationException("Error: I/O Error while invoking blast: "
                + e.getMessage());
        } catch (InterruptedException e) {
            if (formatdb != null)
                formatdb.destroy();
            if (blast != null)
                blast.destroy();
            throw new ApplicationException("Error: I/O Error while invoking blast: "
                + e.getMessage());
        }
    }

    private Cluster findCluster(String taxus) {
        for (int i = 0; i < clusters.size(); ++i)
            if (clusters.get(i).containsTaxus(taxus))
            	return clusters.get(i);
        
        return null;
    }
    
    private Result createResult(AbstractSequence sequence, Cluster cluster, ReferenceTaxus refseq,
                                float score, int start, int end, boolean reverseCompliment) {
    	if (cluster != null)
    		return new Result(sequence, cluster, score, start, end, refseq, reverseCompliment);
    	else
    		return new Result(sequence, null, 0, 0, 0, null, reverseCompliment);
    }

    Result run(SequenceAlignment alignment, AbstractSequence sequence) throws AnalysisException {
        try {
            List<String> sequences = new ArrayList<String>();
            Set<String> clusterSequences = new LinkedHashSet<String>();

            for (int i = 0; i < clusters.size(); ++i) {
                List<String> taxa = clusters.get(i).getTaxaIds();
                clusterSequences.addAll(taxa);
            }
            sequences.addAll(clusterSequences);

            SequenceAlignment analysisDb = alignment.selectSequences(sequences);
            
            return compute(analysisDb, sequence);
        } catch (ApplicationException e) {
            throw new AnalysisException(getId(), sequence, e);
        }
    }

	public Double getCutoff() {
		return cutoff;
	}

	public void addReferenceTaxus(ReferenceTaxus t) {
		referenceTaxa.put(t.getTaxus(), t);
	}

	public Set<String> getRegions() {
		Set<String> result = new HashSet<String>();
		for (ReferenceTaxus t : referenceTaxa.values())
			for (Region r : t.getRegions())
				result.add(r.getName());
		
		return result;
	}
}
