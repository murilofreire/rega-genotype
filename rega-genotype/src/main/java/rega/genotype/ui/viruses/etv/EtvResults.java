/*
 * Copyright (C) 2008 Rega MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.util.GenotypeLib;

/**
 * Utility class to parse and interpret the analysis' results.xml file.
 */
public class EtvResults {
	public static class Conclusion {
		String majorAssignment;
		String majorAssignmentForOverview;
		String majorMotivation;
		String majorBootstrap;
		String variantAssignment;
		String variantDescription;
		String variantBootstrap;
		String variantMotivation;
		String variantAssignmentForOverview;
	}

	public static final String NA = "<i>NA</i>";

	public static Conclusion getConclusion(GenotypeResultParser p) {
		Conclusion result = new Conclusion();

		String conclusionP = "/genotype_result/sequence/conclusion[@id='serotype']";

		if (p.elementExists(conclusionP)) {
			result.majorAssignmentForOverview = GenotypeLib.getEscapedValue(p, conclusionP + "/assigned/id");
			result.majorAssignment = GenotypeLib.getEscapedValue(p, conclusionP + "/assigned/name");
			result.majorBootstrap = GenotypeLib.getEscapedValue(p, conclusionP + "/assigned/support");
			result.majorMotivation = GenotypeLib.getEscapedValue(p, conclusionP + "/motivation");

			String subgenogroupConclusionP = "/genotype_result/sequence/conclusion[@id='subgenogroup']";
			if (p.elementExists(subgenogroupConclusionP)) {
				result.variantAssignment = GenotypeLib.getEscapedValue(p, subgenogroupConclusionP + "/assigned/name");

				boolean showVariantNotAssigned = p.getValue(conclusionP + "/assigned/id").equals("EV-71");
				boolean variantNotAssigned = p.getValue(subgenogroupConclusionP + "/assigned/id").equals("Unassigned");

				if (!variantNotAssigned || showVariantNotAssigned)
					result.variantAssignmentForOverview = result.variantAssignment;

				result.variantBootstrap = GenotypeLib.getEscapedValue(p, subgenogroupConclusionP + "/assigned/support");
				if (!variantNotAssigned)
					result.variantDescription = GenotypeLib.getEscapedValue(p, subgenogroupConclusionP + "/assigned/description");
				else
					result.variantDescription = "Not assigned";
				result.variantMotivation = GenotypeLib.getEscapedValue(p, subgenogroupConclusionP + "/motivation");
			}
		} else {
			result.majorAssignment = "";
			result.majorMotivation = "Sequence does not overlap sufficiently (>100 nucleotides) with VP1";
		}

		return result;
	}
	
	public static String getBlastConclusion(GenotypeResultParser p) {
		return GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/cluster/concluded-name");
	}

	public static String getBlastMotivation(GenotypeResultParser p) {
		return p.elementExists("/genotype_result/sequence/conclusion")
		? GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/motivation")
		: "";
	}
}
