/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.hiv;

import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;

/**
 * HIV implementation of the genotype application.
 * 
 * @author simbre1
 *
 */
@SuppressWarnings("serial")
public class HivMain extends GenotypeMain {
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new HivDefinition(), settings);

		app.useStyleSheet(Settings.defaultStyleSheet);
		
		return app;
	}
}
