package rega.genotype.viruses.dengue;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.ScanAnalysis;

public class DENGUE2SubtypeTool extends GenotypeTool {
    private AlignmentAnalyses dengue2;
    private PhyloClusterAnalysis pureAnalysis;


    public DENGUE2SubtypeTool(File workingDir) throws FileFormatException, IOException, ParameterProblemException {
        dengue2 = readAnalyses("DENGUE/dengue2.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) dengue2.getAnalysis("pure");

    }

    public void analyze(AbstractSequence s) throws AnalysisException {

           PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);

                if (pureresult.haveSupport()) {
                    conclude (pureresult, 
                            "Subtype assigned based on sequence located in the LTR "
                            + "clustering with a DENGUE2 subtype and/or subgroup with bootstrap > 75% ");
                    return;
                } else {
                    conclude ("check the report", "Subtype unassigned based on sequence located in the LTR do not clustering with HTLV1 subtype with bootstrap > 75%.");
                    return;
                }
    
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) dengue2.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }

	@Override
	protected String currentJob() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean cancelAnalysis() {
		// TODO Auto-generated method stub
		return false;
	}

}