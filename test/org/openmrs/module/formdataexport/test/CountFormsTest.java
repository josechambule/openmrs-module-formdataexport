package org.openmrs.module.formdataexport.test;

import java.util.Map;

import org.openmrs.Cohort;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.module.formdataexport.FormDataExportService;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class CountFormsTest extends BaseModuleContextSensitiveTest {

	@Override
	public Boolean useInMemoryDatabase() {
		return false;
	}

	@Override
	protected void onSetUpInTransaction() throws Exception {
		authenticate();
	}

	public void testCountForms() throws Exception {
		FormDataExportService service = (FormDataExportService) Context.getService(FormDataExportService.class);
		
		System.out.println("--------------------\nAll patients\n--------------------");
		Map<Form, Integer> map = service.countForms(null);
		for (Map.Entry<Form, Integer> e : map.entrySet())
			System.out.println(e.getKey().getName() + " -> " + e.getValue());

		Cohort males = Context.getPatientSetService().getPatientsByCharacteristics("M", null, null);
		System.out.println("--------------------\nMales\n--------------------");
		map = service.countForms(males);
		for (Map.Entry<Form, Integer> e : map.entrySet())
			System.out.println(e.getKey().getName() + " -> " + e.getValue());
	}
	
}
