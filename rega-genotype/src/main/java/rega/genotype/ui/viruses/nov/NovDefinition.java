/*
 * Copyright (C) 2008 MyBioData
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.details.DefaultPhylogeneticDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.Genome;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.viruses.nov.NoVTool;
import eu.webtoolkit.jwt.WString;

/**
 * NoV OrganismDefinition implementation.
 */
public class NovDefinition implements OrganismDefinition {
	private Genome genome = new Genome(new NovGenome(this));

	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		NoVTool nrvTool = new NoVTool(jobDir);
		nrvTool.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new NovJobOverview(main);
	}
	
	public String getOrganismDirectory() {
		return "/rega/genotype/ui/viruses/nov/";
	}

	public AbstractDataTableGenerator getDataTableGenerator(AbstractJobOverview jobOverview, DataTable table) throws IOException {
		return new NovTableGenerator(jobOverview, table);
	}

	public Genome getGenome() {
		return genome;
	}

	public IDetailsForm getMainDetailsForm() {
		return new NovSequenceAssignmentForm();
	}

	private void addPhyloDetailForms(GenotypeResultParser p, List<IDetailsForm> forms, String region) {
		String result = "/genotype_result/sequence/result";
		
		String phyloResult = result + "[@id='phylo-" + region + "']";
		if (p.elementExists(phyloResult)) {
			WString title = new WString("Phylogenetic analyses (" + region + ")");
			forms.add(new DefaultPhylogeneticDetailsForm(phyloResult, title, title, true));

			String bestGenotype = GenotypeLib.getEscapedValue(p, phyloResult + "/best/id");
			
			String variantResult = result + "[@id='phylo-" + region + "-" + bestGenotype + "']";
			if (p.elementExists(variantResult)) {
				WString variantTitle = new WString("Phylogenetic analyses (" + region + ") for variant within "
						+ bestGenotype);
				forms.add(new DefaultPhylogeneticDetailsForm(variantResult, variantTitle, variantTitle, true));
			}
		}
	}
	
	public List<IDetailsForm> getSupportingDetailsforms(GenotypeResultParser p) {
		List<IDetailsForm> forms = new ArrayList<IDetailsForm>();

		addPhyloDetailForms(p, forms, "ORF1");
		addPhyloDetailForms(p, forms, "ORF2");
		
		return forms;
	}
	
	public int getUpdateInterval(){
		return 5000;
	}

	public String getOrganismName() {
		return "NoV";
	}

	public boolean haveDetailsNavigationForm() {
		return false;
	}

	public Genome getLargeGenome() {
		return getGenome();
	}

	public String getProfileScanType(GenotypeResultParser p) {
		return null;
	}
}
