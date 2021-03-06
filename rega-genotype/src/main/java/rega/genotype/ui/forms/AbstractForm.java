/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;

/**
 * AbstractForm is the abstract base class for one of the main forms in the rega-genotype UI.
 * 
 * Every form has a 'title' which is used to retrieve the actual title from the resource file,
 * and unless a specific CSS class is given, also to set the CSS class.
 */
public abstract class AbstractForm extends WContainerWidget {
	private WText title;
	private GenotypeWindow main;

	public AbstractForm(GenotypeWindow main, String title) {
		this (main, title, getCssClass(title));
		setObjectName(title);
	}
	
	public AbstractForm(GenotypeWindow main, String title, String cssClass) {
		this.main = main;

		if (title != null) {
			if (!main.getResourceManager().haveForm(title))
				throw new RuntimeException("No '" + title + "' form.");
			setTitle(main.getResourceManager().getOrganismValue(title, "title"));
		}

		this.setStyleClass(cssClass + " form");
	}
	
	public GenotypeWindow getMain() {
		return main;
	}

	private static String getCssClass(String title){
		return title.trim().replace(' ', '_').replace('.','-');
	}
	
	protected void setTitle(WString title) {
		if (this.title != null)
			this.title.remove();

		String titleDiv = "<h1>" + title.getValue() + "</h1>";
		this.title = new WText(titleDiv);
		this.title.setObjectName("title");
		insertWidget(0, this.title);
	}
}
