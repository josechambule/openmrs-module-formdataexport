package org.openmrs.module.formdataexport.db;

import java.util.List;
import java.util.Map;

import org.openmrs.Cohort;
import org.openmrs.Encounter;
import org.openmrs.Form;

public interface FormDataExportDAO {

	public Map<Form, Integer> countForms(Cohort cohort);
	public List<Encounter> getEncountersByForm(Cohort cohort, List<Form> forms);
}
