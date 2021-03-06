/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework;

import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;

import rega.genotype.utils.JobDirCleanTask;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WtServlet;

/**
 * The entry point of the application.
 * 
 * @author simbre1
 *
 */
@SuppressWarnings("serial")
public abstract class GenotypeMain extends WtServlet
{
	protected Settings settings;

	public GenotypeMain() {
		super();
		
		// progressive bootstrap is broken because Wt is not aware of image URLs in documentation forms 
		// getConfiguration().setProgressiveBootstrap(true);
	}
	
	public static GenotypeApplication getApp() {
		return (GenotypeApplication)WApplication.getInstance();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		Settings.initSettings(this.settings = Settings.getInstance(config));
		
		scheduleCleanJobDir();
		
		super.init(config);
	}
	
	private void scheduleCleanJobDir() {
		if (settings.getMaxJobDirLifeTime() == null)
			return;
		
		Class<JobDirCleanTask> job = JobDirCleanTask.class;
		
		String name = "CleanJobDir";
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler scheduler;
		try {
			scheduler = sf.getScheduler();
			scheduler.start();
			JobDetail jobDetail = 
				new JobDetail(name, Scheduler.DEFAULT_GROUP, job);

			SimpleTrigger trigger = new SimpleTrigger(name + "_rega-genotype-trigger_" + job.getClass().toString(),
					Scheduler.DEFAULT_GROUP, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, 24 * 60  *  60 * 1000);
			
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

}