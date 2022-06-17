package org.openmrs.module.formdataexport.db;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openmrs.Cohort;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;

public interface FormDataExportDAO {

	public Map<Form, Integer> countForms(Cohort cohort);

	public List<Encounter> getEncountersByForm(Cohort cohort, List<Form> forms);
	
	public List<Encounter> getEncountersByForm(Cohort cohort, List<Form> form, Date startDate, Date endDate, String firstLast);
	public Cohort getPatientsHavingEncounters(List<EncounterType> encounterTypeList, Location location, Form form,
	        Date fromDate, Date toDate, Integer minCount, Integer maxCount);
	
}
