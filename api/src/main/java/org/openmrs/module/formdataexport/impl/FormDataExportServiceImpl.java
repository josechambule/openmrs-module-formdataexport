package org.openmrs.module.formdataexport.impl;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.formdataexport.FormDataExportService;
import org.openmrs.module.formdataexport.db.FormDataExportDAO;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.export.HtmlFormEntryExportUtil;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.util.FormUtil;
import org.openmrs.util.OpenmrsUtil;

// SIBI
import au.com.bytecode.opencsv.CSVReader;
import java.io.FileOutputStream;
import java.io.StringReader;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Form-based export service.
 * 
 * @author Jembi Health Systems (previous Justin Miranda)
 * @version 1.0.0
 */
public class FormDataExportServiceImpl implements FormDataExportService {

	private Log log = LogFactory.getLog(this.getClass());

	private FormDataExportDAO dao;

	private static String FORM_DATA_EXPORT_PREFIX = "FORM_DATA_EXPORT";

	private static String FORM_DATA_EXPORT_EXTENSION = ".xlsx";

	private static DateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy");

	private static final String DEFAULT_QUOTE = "\"";

	private static final String DEFAULT_COLUMN_SEPARATOR = ",";

	private static final String DEFAULT_LINE_SEPARATOR = "\n";

	private static final int DEFAULT_CONCEPT_NAME_LENGTH = 20;

	private static final String CONCEPT_FIELD_TYPE = "Concept";

	private static final String UNDERSCORE = "_";

	private static final String EMPTY = "";

	public FormDataExportServiceImpl() {
	}

	/**
	 * 
	 * @return
	 */
	public FormDataExportDAO getDao() {
		return dao;
	}

	/**
	 * 
	 * @param dao
	 */
	public void setDao(FormDataExportDAO dao) {
		this.dao = dao;
	}

	/**
	 * Create a file used to store the exported data on the filesystem.
	 * 
	 * @param formName the name of the form
	 */
	public static File createExportFile(String formName) {
		File dir = new File(OpenmrsUtil.getApplicationDataDirectory(), "dataExports");
		dir.mkdirs();
		// String filename = FORM_DATA_EXPORT_PREFIX + "_" + formName.replace(" ", "_")
		// + FORM_DATA_EXPORT_EXTENSION;
		// filename = (new StringBuilder()).append(filename).toString();
		String timestamp = new SimpleDateFormat("yyyyMMdd_Hm").format(new Date());
		String filename = FORM_DATA_EXPORT_PREFIX + UNDERSCORE + formName.replace(" ", "_");
		filename += UNDERSCORE + timestamp + FORM_DATA_EXPORT_EXTENSION;
		File file = new File(dir, filename);
		return file;
	}

	/**
	 * Injects the columns configured in the global properties after the column with
	 * header "afterColumn"
	 * 
	 * @param exportBuffer The export StringBuffer in which to insert the data
	 * @param afterColumn  The title of the column after which to insert the data
	 * @param encounters   The list of encounters for which to add the additional
	 *                     columns
	 */
	public StringBuffer injectConfiguredColumns(StringBuffer exportBuffer, String afterColumn,
			List<Encounter> encounters) {
		StringBuffer ret = new StringBuffer();

		// Flags
		boolean attributeHeadersAdded = false;
		boolean addressHeadersAdded = false;
		boolean addThisColumn = true;

		// Exclusions and exceptions
		List<String> exclusionColumnSuffixes = FormDataExportUtil.getColumnSuffixesToSkipFromGlobalProperty();
		List<String> exclusionExceptions = FormDataExportUtil.getColumnSuffixSkipExclusionsFromGlobalProperty();
		List<Integer> exclusionColumnIndexes = new ArrayList<Integer>();

		// Split the export buffer into rows
		StringTokenizer export = new StringTokenizer(exportBuffer.toString(), DEFAULT_LINE_SEPARATOR);

		// Start by inserting the new titles
		String headerRow = export.nextToken();
		headerRow = FormDataExportUtil.fixCommasInCSVValues(headerRow);

		// Get the person attribute column titles
		List<String> personAttributeTitles = FormDataExportUtil.getPersonAttributeTitlesFromGlobalProperty();

		// Get the person attribute columns
		List<String> personAttributeTypes = FormDataExportUtil.getPersonAttributeTypesFromGlobalProperty();

		// Get the person address column titles
		List<String> personAddressTitles = FormDataExportUtil.getPersonAddressColumnTitlesFromGlobalProperty();

		// Get the person attribute columns
		List<String> personAddressColumns = FormDataExportUtil.getPersonAddressColumnsFromGlobalProperty();

		// Iterate over the titles
		StringTokenizer headerTokens = new StringTokenizer(headerRow, DEFAULT_COLUMN_SEPARATOR);

		// Column counters
		int columnCount = 0;
		int columnInsertIndex = 0;
		int encounterCounter = 0;

		while (headerTokens.hasMoreTokens()) {
			String column = headerTokens.nextToken();

			// Check if this column should be added
			addThisColumn = FormDataExportUtil.shouldAddColumn(column, exclusionColumnSuffixes, exclusionExceptions);
			if (addThisColumn) {
				ret.append(column);
			} else {
				exclusionColumnIndexes.add(columnCount);
			}

			// Count how many columns we've iterated over
			columnCount++;

			if (column.equals(DEFAULT_QUOTE + afterColumn + DEFAULT_QUOTE)) { // Add
																				// the
																				// extra
																				// columns
																				// here

				// Keep the index of the column after which we need to insert
				columnInsertIndex = columnCount;

				// Do a basic check to see if the person attributes global
				// property is well formed
				if ((personAttributeTitles != null) && (personAttributeTypes != null)
						&& (personAttributeTitles.size() == personAttributeTypes.size())) {

					// Add the configured columns
					for (String attributeTitle : personAttributeTitles) {
						ret.append(DEFAULT_COLUMN_SEPARATOR).append(DEFAULT_QUOTE).append(attributeTitle)
								.append(DEFAULT_QUOTE);
						columnCount++;
					}

					attributeHeadersAdded = true;
					addThisColumn = true;
				}

				// Do a basic check to see if the global property is well formed
				if ((personAddressTitles != null) && (personAddressColumns != null)
						&& (personAddressTitles.size() == personAddressColumns.size())) {

					// Add the configured columns
					for (String addressTitle : personAddressTitles) {
						ret.append(DEFAULT_COLUMN_SEPARATOR).append(DEFAULT_QUOTE).append(addressTitle)
								.append(DEFAULT_QUOTE);
						columnCount++;
					}

					addressHeadersAdded = true;
					addThisColumn = true;
				}
			}
			if (addThisColumn) {
				ret.append(DEFAULT_COLUMN_SEPARATOR);
			}
		}
		ret.append(DEFAULT_LINE_SEPARATOR);

		// Make sure we actually added or removed something
		if ((attributeHeadersAdded == false) && (addressHeadersAdded == false) && (exclusionColumnSuffixes == null)) {
			return exportBuffer;
		}

		// Iterate over the encounter rows
		while (export.hasMoreTokens()) {

			String row = FormDataExportUtil.fixCommasInCSVValues(export.nextToken());
			StringTokenizer rowTokens = new StringTokenizer(row, DEFAULT_COLUMN_SEPARATOR);

			int columnCounter = 0;
			Encounter e = encounters.get(encounterCounter++);

			while (rowTokens.hasMoreTokens()) {
				String column = rowTokens.nextToken();

				// Make sure we need to add this column
				addThisColumn = !exclusionColumnIndexes.contains(columnCounter);

				if (columnCounter++ == columnInsertIndex) { // Inject our data
															// here
					if (attributeHeadersAdded) { // Add patient attribute data
						for (String attributeType : personAttributeTypes) {
							ret.append(DEFAULT_QUOTE);
							ret.append(FormDataExportUtil.getPersonAttributeValue(attributeType, e.getPatient()));
							ret.append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
							columnCounter++;
						}
					}

					if (addressHeadersAdded) { // Add patient address data
						for (String addressColumn : personAddressColumns) {
							ret.append(DEFAULT_QUOTE);
							ret.append(FormDataExportUtil.getPersonAddressValue(addressColumn, e.getPatient()));
							ret.append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
							columnCounter++;
						}
					}

				}

				if (addThisColumn) {
					ret.append(column).append(DEFAULT_COLUMN_SEPARATOR);
				}
			}
			ret.append(DEFAULT_LINE_SEPARATOR);
		}

		return ret;
	}

	public File exportEncounterData(Form form, Integer sectionIndex, Date startDate, Date endDate,
			String cohortQueryUUID, List<String> extraCols, String firstLast, Integer quantity) {

		// setup the cohort:
		Cohort cohort = new Cohort();
		if (cohortQueryUUID != null && !cohortQueryUUID.equals("")) {
			CohortDefinition cd = Context.getService(CohortDefinitionService.class)
					.getDefinitionByUuid(cohortQueryUUID);
			if (cd != null) {
				// TODO: add parameters input to this module's UI?
				EvaluationContext ev = new EvaluationContext();
				try {
					cohort = Context.getService(CohortDefinitionService.class).evaluate(cd, ev);
				} catch (EvaluationException e) {
					log.fatal("Error evaluating cohort: " + e.getMessage());
				}
			}
		} else {
			List<EncounterType> encounterTypes = new ArrayList<EncounterType>();
			encounterTypes.add(form.getEncounterType());
			log.info("Using patients with encounters");
			cohort = dao.getPatientsHavingEncounters(Collections.singletonList(form.getEncounterType()), null, form,
					null, null, null, null);
		}

		StringBuffer exportBuffer = new StringBuffer();

		// Get all encounters for the given form
		// NOTE: firstLast only affects sort order. If no quantity given, and
		// first or last requested,
		// set quantity to 1 by default
		if (quantity == null && firstLast != null && !firstLast.equals(""))
			quantity = 1;

		List<Encounter> encounters = dao.getEncountersByForm(cohort, Collections.singletonList(form), startDate,
				endDate, firstLast);
		// Filter by quantity if given:
		if (quantity != null) {
			Map<Patient, List<Encounter>> map = new LinkedHashMap<Patient, List<Encounter>>();
			for (Encounter enc : encounters) {
				if (!map.containsKey(enc.getPatient())) {
					ArrayList<Encounter> eList = new ArrayList<Encounter>();
					eList.add(enc);
					map.put(enc.getPatient(), eList);
				} else {
					List<Encounter> eList = map.get(enc.getPatient());
					if (eList.size() < quantity) {
						eList.add(enc);
						map.put(enc.getPatient(), eList);
					}
				}
			}
			List<Encounter> ret = new ArrayList<Encounter>();
			for (Map.Entry<Patient, List<Encounter>> e : map.entrySet()) {
				for (Encounter enc : e.getValue()) {
					ret.add(enc);
				}
			}
			encounters = ret;
		}

		HtmlForm htmlForm = Context.getService(HtmlFormEntryService.class).getHtmlFormByForm(form);
		// filter by section if requested
		if (sectionIndex != null && htmlForm != null) {
			try {
				String newXML = HtmlFormEntryExportUtil.getSectionAsFormXml(htmlForm, sectionIndex);
				htmlForm.setXmlData(newXML);
				List<Encounter> ret = new ArrayList<Encounter>();
				for (Encounter enc : encounters) {
					ret.add(HtmlFormEntryExportUtil.trimEncounterToMatchForm(enc, htmlForm));
				}
				encounters = ret;
			} catch (Exception ex) {
				throw new RuntimeException("Unable to trim htmlform by section.", ex);
			}
		}

		if (htmlForm != null) {

			List<PatientIdentifierType> pits = FormDataExportUtil.getPatientIdentifierTypesFromGlobalProperty();

			// HTMLFORM
			HtmlFormEntryExportUtil.buildHtmlFormExport(encounters, htmlForm, extraCols, exportBuffer, new Locale("en"),
					pits);

			// Inject Configured Columns
			exportBuffer = injectConfiguredColumns(exportBuffer, pits.get(pits.size() - 1).getName(), encounters);
		} else {
			// INFOPATH
			// Create a map to keep track of the data
			Map<Encounter, Map<Concept, List<Obs>>> dataMap = new HashMap<Encounter, Map<Concept, List<Obs>>>();

			// Keeps track of the maximum number of columns to create for each
			// concept
			Map<Concept, Integer> fieldMap = new HashMap<Concept, Integer>();

			// Create the dataset maps
			populateMaps(dataMap, fieldMap, encounters);

			// Write the column headers to the export buffer
			writeColumnHeaders(form, fieldMap, exportBuffer, extraCols);

			// Write the column data to the export buffer
			writeColumnData(form, dataMap, fieldMap, exportBuffer, extraCols);
		}
		File exportFile = null;
		exportFile = createExportFile(form.getName());

		CSVReader reader = new CSVReader(new StringReader(exportBuffer.toString()));

		String[] csvLineString;
		String sheetName = form.getName();

		XSSFWorkbook wb = new XSSFWorkbook();
		XSSFSheet sheet = wb.createSheet(sheetName);

		int lineNumber = 0;

		try {
			while ((csvLineString = reader.readNext()) != null) {
				XSSFRow row = sheet.createRow(lineNumber);

				if (lineNumber == 0) {
					// do stuff
				} else {
					// do stuff
				}

				for (int i = 0; i < csvLineString.length; i++) {
					XSSFCell cell = row.createCell(i);
					cell.setCellValue(csvLineString[i]);
				}

				lineNumber += 1;
			}

			FileOutputStream exportOutput = new FileOutputStream(exportFile);
			wb.write(exportOutput);
			exportOutput.flush();
			exportOutput.close();

			reader.close(); // closes the OpenCSV reader

			// Writer exportOutput = new BufferedWriter( new FileWriter(exportFile) );
			// exportOutput.write( exportBuffer.toString() );
			// Close output
			// exportOutput.close();
		} catch (Exception ex) {
			log.error("Could not export file");
		}
		return exportFile;
	}

	/**
	 * 
	 * @param dataMap
	 * @param fieldMap
	 * @param encounters
	 */
	public void populateMaps(Map<Encounter, Map<Concept, List<Obs>>> dataMap, Map<Concept, Integer> fieldMap,
			List<Encounter> encounters) {
		// Iterate over all encounters
		for (Encounter encounter : encounters) {

			// Get all of the observations for a given encounter
			Map<Concept, List<Obs>> obsListMap = dataMap.get(encounter);
			if (obsListMap == null)
				obsListMap = new HashMap<Concept, List<Obs>>();

			// If the encounter has observations, we process them
			if (encounter.getObs() != null && !encounter.getObs().isEmpty()) {

				// Iterate through the list of observations for this encounter
				for (Obs obs : encounter.getObs()) {

					if (obs != null && !obs.isVoided()) {
						/*
						 * log.info("\tObs " + obs + " " + obs.getConcept().getName() + "(" +
						 * obs.getConcept().getConceptId() + ") = " + obs.getValueAsString(Locale.US));
						 */
						// Get the concept and it's observations
						Concept concept = obs.getConcept();
						List<Obs> obsList = obsListMap.get(concept);

						// If we haven't encountered this concept before, we
						// create a new observation list
						if (obsList == null)
							obsList = new LinkedList<Obs>();

						// Add the current observation to the list
						obsList.add(obs);

						// Add the current concept's obs list to the map
						obsListMap.put(obs.getConcept(), obsList);

						// Since we might have multiple obs values for one
						// concept in a single
						// encounter, we need keep track of the max
						// number of observations for a single concept over the
						// entire data set
						//
						// This is used when displaying columns for the a
						// concept that may have
						// multiple observations within a single row. In order
						// to display all
						// values, we need all rows to display the same number
						// of columns even
						// if most rows have less than the max. In the following
						// example, the
						// row with ID = 1 has 3 obs values, so for each row we
						// need to display
						// 3 columns for this particular concept.
						//
						//
						//
						// ID COL_1 COL_2 COL_3
						// 1 129.0 185.0 248.0
						// 2 492.0
						// 3 124.0 482.0
						//
						Integer currentObsListSize = obsList.size();
						Integer maximumObsListSize = (fieldMap.get(concept) != null) ? fieldMap.get(concept) : 0;
						if (currentObsListSize > maximumObsListSize) {
							// log.info(" - MaxSize for " + concept.getName() +
							// " = " + currentObsListSize);
							fieldMap.put(concept, currentObsListSize);
						}
					}
				}
			}
			// Include encounters with no observations
			dataMap.put(encounter, obsListMap);
		}

	}

	/**
	 * Write out the form column headers for the export.
	 * 
	 * @param form
	 * @param exportBuffer
	 */
	public void writeColumnHeaders(Form form, Map<Concept, Integer> fieldMap, StringBuffer exportBuffer,
			List<String> extraCols) {

		// Write out static columns
		exportBuffer.append(DEFAULT_QUOTE).append("ENCOUNTER ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR)
				.append(DEFAULT_QUOTE).append("ENCOUNTER DATE").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR)
				.append(DEFAULT_QUOTE).append("ENCOUNTER LOCATION").append(DEFAULT_QUOTE)
				.append(DEFAULT_COLUMN_SEPARATOR).append(DEFAULT_QUOTE).append("ENCOUNTER PROVIDER")
				.append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).append(DEFAULT_QUOTE)
				.append("INTERNAL PATIENT ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
		for (PatientIdentifierType pit : FormDataExportUtil.getPatientIdentifierTypesFromGlobalProperty()) {
			exportBuffer.append(DEFAULT_QUOTE).append(pit.getName()).append(DEFAULT_QUOTE)
					.append(DEFAULT_COLUMN_SEPARATOR);
		}

		// IF INFOPATH

		TreeMap<Integer, TreeSet<FormField>> formStructure = (TreeMap<Integer, TreeSet<FormField>>) FormUtil.getFormStructure(form);
		StringBuffer fieldNumber = new StringBuffer();
		// Iterate over high level nodes
		for (Integer id : formStructure.keySet()) {
			FormField parent = Context.getFormService().getFormField(id);
			fieldNumber.setLength(0);
			if (parent != null) {
				log.info("Parent form field: " + parent.getFormFieldId() + " " + parent.getFieldNumber());
			} else {
				log.info("Parent id: " + id);
			}
			// Iterate over the actual questions
			TreeSet<FormField> formFields = formStructure.get(id);
			if (formFields != null && !formFields.isEmpty()) {
				for (FormField formField : formFields) {
					Field field = formField.getField();
					// Only process concept form fields
					if (CONCEPT_FIELD_TYPE.equals(field.getFieldType().getName())) {
						// Create a column name based on the concept
						Concept concept = field.getConcept();
						// Check if concept exists (it should)
						if (concept != null) {
							int numberOfOccurrences = (fieldMap.get(concept) != null) ? fieldMap.get(concept) : 1;
							for (int occurrence = 1; occurrence <= numberOfOccurrences; occurrence++) {
								// If concept is a concept set, we'll ignore it because we'll see it later
								if (concept.getConceptSets() != null && !concept.getConceptSets().isEmpty()) {
									/*
									 * for (ConceptSet cs : formField.getField().getConcept().getConceptSets()) {
									 * log.info("\t - Concept Set Field: " + field.getFieldId() + " - " +
									 * cs.getConcept().getConceptId() + " " + field.getName());
									 * addColumnHeader(exportBuffer, fieldNumber.toString(), cs.getConcept(), i,
									 * false); }
									 */
								} else {
									// Otherwise, just create a new column for the concept
									String fieldName = createColumnHeader(formField, occurrence);
									log.debug("\tConcept Field: " + fieldName);
									addColumnHeader(exportBuffer, fieldName, false, extraCols);

								}
							}
						}
					}
				}
			}
		}

		exportBuffer.append("\n");
		log.info("Column Buffer: " + exportBuffer.toString());
	}

	/**
	 * Creates a column header string for the given form field, as well as the
	 * number of times it appears in the form.
	 * 
	 * @param formField
	 * @param occurrence
	 * @return
	 */
	public String createColumnHeader(FormField formField, int occurrence) {

		StringBuffer buffer = new StringBuffer();

		// Try to get the field number from the current field
		String fieldNumber = getFieldNumber(formField);

		// If there is no field number then we try to get the field number for the
		// parent
		if (fieldNumber == null || EMPTY.equals(fieldNumber)) {
			if (formField.getParent() != null) {
				fieldNumber = getFieldNumber(formField.getParent());
			}
		}
		buffer.append(fieldNumber);

		Concept concept = formField.getField().getConcept();

		// By default set field name, but try to use concept short name
		String fieldName = concept.getName(Context.getLocale()).getName();

		// Replace unwanted characters and change case to upper
		fieldName = fieldName.replaceAll("\\s", "_");
		fieldName = fieldName.replaceAll("-", "_");
		fieldName = fieldName.toUpperCase();

		// Now we make sure that the field fits in the
		if (fieldName.length() > DEFAULT_CONCEPT_NAME_LENGTH) {
			buffer.append(fieldName.substring(0, DEFAULT_CONCEPT_NAME_LENGTH));
		} else {
			buffer.append(fieldName);
		}
		buffer.append("_").append(concept.getConceptId());

		// If the concept occurs in the form more than once, then we need to distinguish
		// it's columns
		if (occurrence > 1) {
			buffer.append("_").append(occurrence);
		}

		return buffer.toString();
	}

	/**
	 * Gets the field number of the given form field as a string.
	 * 
	 * @param formField
	 * @return
	 */
	private String getFieldNumber(FormField formField) {
		StringBuffer buffer = new StringBuffer();
		// Get the field number and part
		if (formField != null) {
			if (formField.getFieldNumber() != null && !EMPTY.equals(formField.getFieldNumber())) {
				buffer.append(formField.getFieldNumber());
				if (formField.getFieldPart() != null && !EMPTY.equals(formField.getFieldPart())) {
					buffer.append(formField.getFieldPart());
				}
				buffer.append(UNDERSCORE);
			}
		}
		return buffer.toString();
	}

	/**
	 * Write out a single column header to the export buffer.
	 * 
	 * @param buffer
	 * @param fieldNumber
	 * @param concept
	 * @param isLast
	 */
	public void addColumnHeader(StringBuffer exportBuffer, String columnHeader, boolean isLast,
			List<String> extraCols) {

		// Add quotes around the column header
		exportBuffer.append(DEFAULT_QUOTE);
		exportBuffer.append(columnHeader);
		exportBuffer.append(DEFAULT_QUOTE);

		exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);

		exportBuffer.append(DEFAULT_QUOTE);
		exportBuffer.append(columnHeader + "_DATE");
		exportBuffer.append(DEFAULT_QUOTE);

		if (extraCols != null) {
			for (String st : extraCols) {
				if (st.equals("valueModifier")) {
					exportBuffer.append(DEFAULT_QUOTE);
					exportBuffer.append(columnHeader + "_ValueModifier");
					exportBuffer.append(DEFAULT_QUOTE);
					exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
				} else if (st.equals("accessionNumber")) {
					exportBuffer.append(DEFAULT_QUOTE);
					exportBuffer.append(columnHeader + "_AccessionNumber");
					exportBuffer.append(DEFAULT_QUOTE);
					exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
				} else if (st.equals("comment")) {
					exportBuffer.append(DEFAULT_QUOTE);
					exportBuffer.append(columnHeader + "_Comment");
					exportBuffer.append(DEFAULT_QUOTE);
					exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
				}
			}
		}

		// TODO: Are obs groups handled correctly?

		// Add comma if this isn't the last comma
		if (!isLast)
			exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);

	}

	/**
	 * Write all columns of data. Columns are separated by commas, rows are
	 * separated by newlines.
	 * 
	 * @param form
	 * @param dataMap
	 * @param fieldMap
	 * @param exportBuffer
	 */
	public void writeColumnData(Form form, Map<Encounter, Map<Concept, List<Obs>>> dataMap,
			Map<Concept, Integer> fieldMap, StringBuffer exportBuffer, List<String> extraCols) {

		// Write out all of the rows
		for (Encounter row : dataMap.keySet()) {
			// Add static column data
			Patient patient = row.getPatient();
//			StringBuilder sb = new StringBuilder("");
//			int count = 0;
//			if (patient != null && patient.getActiveIdentifiers() != null){
//    			for (PatientIdentifier pi : patient.getActiveIdentifiers()){
//    			    if (count != 0)
//    			        sb.append(",");    
//    			    sb.append(pi.getIdentifierType().getName() + ":" + pi.getIdentifier());
//    			    count++;
//    			}
//			}	

			exportBuffer.append(DEFAULT_QUOTE).append(row.getEncounterId()).append(DEFAULT_QUOTE)
					.append(DEFAULT_COLUMN_SEPARATOR);
			exportBuffer.append(DEFAULT_QUOTE).append(DATE_FORMATTER.format(row.getEncounterDatetime()))
					.append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
			exportBuffer.append(DEFAULT_QUOTE).append(row.getLocation().getName()).append(DEFAULT_QUOTE)
					.append(DEFAULT_COLUMN_SEPARATOR);
			exportBuffer.append(DEFAULT_QUOTE)
					.append(row.getCreator().getGivenName() + " " + row.getCreator().getFamilyName())
					.append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
			exportBuffer.append(DEFAULT_QUOTE).append((patient != null ? patient.getPatientId() : ""))
					.append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
			for (PatientIdentifierType pit : FormDataExportUtil.getPatientIdentifierTypesFromGlobalProperty()) {
				exportBuffer.append(DEFAULT_QUOTE).append(patient.getPatientIdentifier(pit)).append(DEFAULT_QUOTE)
						.append(DEFAULT_COLUMN_SEPARATOR);
			}
			Map<Concept, List<Obs>> columnData = dataMap.get(row);

			TreeMap<Integer, TreeSet<FormField>> formStructure = (TreeMap<Integer, TreeSet<FormField>>) FormUtil.getFormStructure(form);

			// Iterate over high level nodes
			for (Integer id : formStructure.keySet()) {

				// Iterate over the actual questions
				TreeSet<FormField> formFields = formStructure.get(id);

				// Iterate through the fields in the form and add the concepts to the export
				for (FormField formField : formFields) {

					if ("Concept".equals(formField.getField().getFieldType().getName())) {

						Concept concept = formField.getField().getConcept();

						if (concept != null) {

							int numberOfOccurrences = (fieldMap.get(concept) != null) ? fieldMap.get(concept) : 1;

							for (int i = 1; i <= numberOfOccurrences; i++) {

								// If we encounter a concept set, we ignore it because we run into it later
								if (concept.getConceptSets() != null && !concept.getConceptSets().isEmpty()) {
									/*
									 * // Then we iterate over concepts in concept set and display values for
									 * (ConceptSet cs : formField.getField().getConcept().getConceptSets()) {
									 * 
									 * 
									 * exportBuffer. append(DEFAULT_QUOTE);
									 * 
									 * // If the observation list is not empty, we'll write the first observation we
									 * find in the list List<Obs> obsList = columnData.get(cs.getConcept()); if
									 * (obsList != null && !obsList.isEmpty()) {
									 * exportBuffer.append(obsList.get(0).getValueAsString(Locale.US));
									 * 
									 * // NOTE - we need to remove observations from this list as we write them to
									 * the export buffer obsList.remove(0); }
									 * 
									 * exportBuffer. append(DEFAULT_QUOTE). append(DEFAULT_COLUMN_SEPARATOR); }
									 */
								} else {

									// If the observation list is not empty, display the first value
									Obs obs = null;

									List<Obs> obsList = columnData.get(concept);
									if (obsList != null && !obsList.isEmpty()) {
										obs = obsList.get(0);
										// NOTE - we need to remove observations from this list as we write
										// them to the export buffer
										//
										// TODO Figure out why we have to do this?
										obsList.remove(0);
									}

									// Write out the observation value
									exportBuffer.append(DEFAULT_QUOTE);
									exportBuffer.append((obs != null) ? obs.getValueAsString(Context.getLocale()) : "");
									exportBuffer.append(DEFAULT_QUOTE);
									exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);

									// Write out observation date
									exportBuffer.append(DEFAULT_QUOTE);
									exportBuffer.append(
											(obs != null) ? Context.getDateFormat().format(obs.getObsDatetime()) : "");
									exportBuffer.append(DEFAULT_QUOTE);
									exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);

									// TODO: write out extra Obs properties:
									// value_modifier, accession_number, comment
									if (extraCols != null) {
										for (String st : extraCols) {
											if (st.equals("valueModifier")) {
												exportBuffer.append(DEFAULT_QUOTE);
												exportBuffer.append((obs != null) ? obs.getValueModifier() : "");
												exportBuffer.append(DEFAULT_QUOTE);
												exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
											} else if (st.equals("accessionNumber")) {
												exportBuffer.append(DEFAULT_QUOTE);
												exportBuffer.append((obs != null) ? obs.getAccessionNumber() : "");
												exportBuffer.append(DEFAULT_QUOTE);
												exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
											} else if (st.equals("comment")) {
												exportBuffer.append(DEFAULT_QUOTE);
												exportBuffer.append((obs != null) ? obs.getComment() : "");
												exportBuffer.append(DEFAULT_QUOTE);
												exportBuffer.append(DEFAULT_COLUMN_SEPARATOR);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			exportBuffer.append("\n");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.module.formdataexport.FormDataExportService#countForms(org.
	 * openmrs.Cohort)
	 */
	public Map<Form, Integer> countForms(Cohort patients) {
		return dao.countForms(patients);
	}

	/**
	 * Write out the form column headers for the export.
	 * 
	 * @param form
	 * @param exportBuffer public void writeFormColumnData(Form form, StringBuffer
	 *                     exportBuffer) {
	 * 
	 *                     TreeMap<Integer, TreeSet<FormField>> formStructure =
	 *                     FormUtil.getFormStructure(form);
	 * 
	 *                     StringBuffer fieldNumber = new StringBuffer();
	 * 
	 *                     // Iterate over high level nodes for(Integer id :
	 *                     formStructure.keySet()) {
	 * 
	 *                     // Iterate over the actual questions TreeSet<FormField>
	 *                     formFields = formStructure.get(id);
	 * 
	 *                     for(FormField formField : formFields) { Field field =
	 *                     formField.getField();
	 * 
	 *                     if ("Concept".equals(field.getFieldType().getName())) {
	 * 
	 *                     // Get the field number and part if
	 *                     (formField.getFieldNumber()!=null) {
	 *                     fieldNumber.append(formField.getFieldNumber()); if
	 *                     (formField.getFieldPart()!=null)
	 *                     fieldNumber.append(formField.getFieldPart()); }
	 * 
	 *                     // Check if concept exists (it should) if
	 *                     (field.getConcept() != null) {
	 * 
	 *                     // Create a column name based on the concept Concept
	 *                     concept = field.getConcept();
	 *                     addColumnHeader(exportBuffer, fieldNumber.toString(),
	 *                     concept, false);
	 * 
	 *                     if ( concept.getConceptSets()!= null) { for (ConceptSet
	 *                     cs : formField.getField().getConcept().getConceptSets())
	 *                     { addColumnHeader(exportBuffer, fieldNumber.toString(),
	 *                     cs.getConcept(), false);
	 * 
	 *                     } } } } fieldNumber.setLength(0);
	 * 
	 *                     }
	 * 
	 *                     }
	 * 
	 *                     log.info("Column Buffer: " + exportBuffer.toString()); }
	 */

	/**
	 * Adds columns to the based on concepts related to the specified form.
	 * 
	 * @param export
	 * @param form
	 * @param extras public void addFormColumns(DataExportReportObject export, Form
	 *               form, String [] extras) {
	 * 
	 *               // Calculate the number of times each concept appears in the
	 *               form Map<Concept, Integer> hitsByConcept =
	 *               getHitsByConcept(form);
	 * 
	 *               //Order the fields by their field number and field part
	 *               //Comparator<FormField> comparator = new FormFieldComparator();
	 *               List<FormField> formFields = new LinkedList<FormField>();
	 *               formFields.addAll(form.getFormFields());
	 *               Collections.sort(formFields);
	 * 
	 *               StringBuffer columnName = new StringBuffer();
	 * 
	 *               // Iterate through the fields in the form and add the concepts
	 *               to the export if ( formFields != null ) { for (FormField
	 *               formField : formFields) { Concept concept =
	 *               formField.getField().getConcept();
	 * 
	 *               if (concept != null) {
	 * 
	 *               // Figure out how many times this concept occurs for this form
	 *               Integer hits = hitsByConcept.get(concept);
	 * 
	 *               // Reset column name buffer columnName.setLength(0);
	 * 
	 *               // Get column name
	 * 
	 * 
	 * 
	 *               // Add the first level of concepts in a concept set
	 *               (sub-questions) // Append the column names with concept ID to
	 *               differentiate since the column name // will start with the
	 *               column name of the top-level question Collection<ConceptSet>
	 *               conceptSets = concept.getConceptSets(); if (conceptSets != null
	 *               && !conceptSets.isEmpty()) { StringBuffer buffer = new
	 *               StringBuffer(); for (ConceptSet conceptSet : conceptSets) {
	 *               buffer.setLength(0); buffer.append(getColumnName(formField));
	 *               buffer.append(columnName.toString()). append("_").
	 *               append(conceptSet.getConcept().getConceptId());
	 * 
	 *               ConceptColumn column = createColumn(buffer.toString(),
	 *               conceptSet.getConcept(), hits, extras);
	 * 
	 *               export.getColumns().add(column);
	 * 
	 *               } } // Otherwise just add the concept that goes along with it
	 *               else {
	 * 
	 *               columnName.append(getColumnName(formField));
	 * 
	 *               ConceptColumn column = createColumn(columnName.toString(),
	 *               concept, hits, extras);
	 * 
	 *               export.getColumns().add(column); }
	 *               hitsByConcept.remove(concept); }
	 * 
	 *               } } }
	 */

	/**
	 * Create a column based on the concept representing the question that is being
	 * asked.
	 * 
	 * @param name
	 * @param concept
	 * @param occurs
	 * @param extras
	 * @return public ConceptColumn createColumn(String name, Concept concept,
	 *         Integer occurs, String [] extras) { ConceptColumn column = null;
	 *         System.out.println("Name: " + name + ", Concept: " + concept + ",
	 *         Occurs: " + occurs);
	 * 
	 *         column = new ConceptColumn(); column.setColumnName(name.toString());
	 *         column.setConceptId(concept.getConceptId());
	 *         column.setExtras(extras);
	 * 
	 *         if (occurs != null && occurs > 1) {
	 *         column.setModifier(DataExportReportObject.MODIFIER_LAST_NUM);
	 *         column.setModifierNum(occurs); } else {
	 *         column.setModifier(DataExportReportObject.MODIFIER_LAST); } return
	 *         column; }
	 */

	/**
	 * Creates a map of concepts with the number of times they appear in a given
	 * form.
	 * 
	 * TODO Currently only supports high level concepts.
	 * 
	 * @param form the form to process
	 * @return a map that keeps track of the number of times a concept occurs in a
	 *         form public Map<Concept, Integer> getHitsByConcept(Form form) {
	 *         Map<Concept, Integer> map = new HashMap<Concept, Integer>();
	 * 
	 *         System.out.println("There are " + form.getFormFields().size() + "
	 *         fields on this form"); for (FormField formField :
	 *         form.getFormFields()) {
	 * 
	 * 
	 *         Concept concept = formField.getField().getConcept(); if (concept !=
	 *         null) { System.out.println("Concept " +
	 *         concept.getName().getShortName()); if (concept.getConceptSets() !=
	 *         null && !concept.getConceptSets().isEmpty()) { for (ConceptSet set :
	 *         concept.getConceptSets()) { Concept child = set.getConcept();
	 *         System.out.println("\tConcept " + child.getName().getShortName());
	 *         Integer hits = map.get(child); if (hits == null) hits = new
	 *         Integer(0); hits++; map.put(concept, hits); } } else { Integer hits =
	 *         map.get(concept); if (hits == null) hits = new Integer(0); hits++;
	 *         map.put(concept, hits); } } } return map; }
	 */

	/**
	 * 
	 * @param formField
	 * @return public String getColumnName(FormField formField) {
	 * 
	 *         StringBuffer buffer = new StringBuffer();
	 * 
	 *         // Add field number and field part if they exist if
	 *         (formField.getFieldNumber() != null ) {
	 *         buffer.append(formField.getFieldNumber()); if
	 *         (formField.getFieldPart() != null )
	 *         buffer.append(formField.getFieldPart()); buffer.append(" "); }
	 * 
	 *         try { String shortName =
	 *         formField.getField().getConcept().getName().getShortName();
	 * 
	 * 
	 *         if (shortName != null && !shortName.isEmpty() )
	 *         buffer.append(shortName); else buffer.append("Concept
	 *         ").append(formField.getField().getConcept().getConceptId());
	 * 
	 *         } catch (Exception e) { log.info("Concept short name is null, using
	 *         field number/field part or concept name"); }
	 * 
	 * 
	 *         return buffer.toString();
	 * 
	 *         }
	 */

	/*
	 * public File exportFormData(Form form, PatientSet patients, String []
	 * extraColumns) throws Exception {
	 * 
	 * //Form form = Context.getFormService().getForm(formId);
	 * 
	 * 
	 * if ( form == null ) { throw new Exception("Form cannot be null"); } else {
	 * 
	 * // Create new export DataExportReportObject export = new
	 * DataExportReportObject(); export.setName(FORM_DATA_EXPORT_PREFIX + "_" +
	 * form.getName());
	 * 
	 * // Populate export with columns addStaticColumns(export);
	 * 
	 * // Populate export with columns from the form addFormColumns(export, form,
	 * extraColumns);
	 * 
	 * // If patients not passed in, set the cohort to all patients having
	 * encounters if (patients == null || patients.getSize() <= 0) patients =
	 * getPatientsHavingEncounters(form);
	 * 
	 * // If patients is still null, set cohort to all patients if (patients == null
	 * || patients.getSize() <= 0) patients =
	 * Context.getPatientSetService().getAllPatients();
	 * 
	 * log.info("Patients: " + patients);
	 * 
	 * // Export the data as a TSV file DataExportUtil.generateExport(export,
	 * patients); File tempExportFile = DataExportUtil.getGeneratedFile(export);
	 * 
	 * // Convert the file to CSV String tsv = tempExportFile.getCanonicalPath();
	 * String csv = FormDataExportUtil.changeExtension(tsv, "csv");
	 * FormDataExportUtil.convertFormat(tsv, "\t", csv, ",");
	 * 
	 * return new File(csv);
	 * 
	 * 
	 * }
	 * 
	 * }
	 */

}

/**
 * 
 */
class FormFieldComparator implements Comparator<FormField> {
	public int compare(FormField ff1, FormField ff2) {
		int fieldNumber1 = ff1.getFieldNumber() != null ? ff1.getFieldNumber() : 0;
		int fieldNumber2 = ff2.getFieldNumber() != null ? ff2.getFieldNumber() : 0;
		int compare = fieldNumber1 - fieldNumber2;
		if (compare == 0) {
			String fieldPart1 = ff1.getFieldPart() != null ? ff1.getFieldPart() : "";
			String fieldPart2 = ff2.getFieldPart() != null ? ff2.getFieldPart() : "";
			compare = fieldPart1.compareTo(fieldPart2);
		}
		return compare;
	}
}
