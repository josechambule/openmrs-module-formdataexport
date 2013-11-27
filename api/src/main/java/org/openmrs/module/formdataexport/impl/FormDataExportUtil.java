package org.openmrs.module.formdataexport.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.context.Context;

public class FormDataExportUtil {
	/* Logger */
	private static Log log = LogFactory.getLog(FormDataExportUtil.class);

	private static final String GLOBAL_PROPERTY_TITLE_SEPARATOR = "=";
	private static final String GLOBAL_PROPERTY_EXCLUSION_SEPARATOR = "|";
	private static final String GLOBAL_PROPERTY_DEFAULT_SEPARATOR = ",";
	private static final String DEFAULT_QUOTE = "\"";
	private static final String COLUMN_NAME_SEPARATOR_REGEX = DEFAULT_QUOTE
			+ GLOBAL_PROPERTY_DEFAULT_SEPARATOR + DEFAULT_QUOTE;
	private static final String CSV_VALUE_COMMA_REPLACEMENT = ":";
	private static final String ROBUST_SEPARATOR = "|||";

	public static final Set<String> ADDRESS_COLUMNS = new HashSet<String>(
			Arrays.asList(new String[] { "address1", "address2", "address3",
					"address4", "address5", "address6", "cityVillage",
					"countyDistrict", "stateProvince", "country", "postalCode",
					"latitude", "longitude" }));

	public static final String SPECIAL_PERSON_ATTRIBUTE_AGE = "age";
	public static final String SPECIAL_PERSON_ATTRIBUTE_GENDER = "gender";
	public static final String SPECIAL_PERSON_ATTRIBUTE_GIVEN_NAME = "givenName";
	public static final String SPECIAL_PERSON_ATTRIBUTE_FAMILY_NAME = "familyName";

	public static final String DATE_OF_BIRTH_FORMAT = "dd/MM/yyyy";

	/**
	 * Converts the separator in the file from any separator to any other
	 * separator. This method also adds quotes around all columns.
	 * 
	 * TODO This should be configurable.
	 * 
	 * @param fromFilename
	 *            full path to the input file
	 * @param fromSeparator
	 *            string representing the separator to convert from
	 * @param toFilename
	 *            full path to the output file
	 * @param toSeparator
	 *            string representing the separator to convert to
	 */
	public static boolean convertFormat(String fromFilename,
			String fromSeparator, String toFilename, String toSeparator) {

		DataOutputStream output = null;
		BufferedReader input = null;

		try {

			StringBuffer buffer = new StringBuffer();
			input = new BufferedReader(new FileReader(fromFilename));
			output = new DataOutputStream(new FileOutputStream(toFilename));

			// Iterate over the lines in the file
			String line = null;
			while ((line = input.readLine()) != null) {

				// Get all column values for a single line
				String[] columns = line.split(fromSeparator);
				if (columns != null) {

					// Iterate over all columns for a single line
					for (int i = 0; i < columns.length; i++) {
						// Add leading and trailing quotes
						buffer.append("\"").append(columns[i].trim())
								.append("\"");

						// If not the last column, add the new separator (comma,
						// pipe, etc)
						if (i < columns.length - 1)
							buffer.append(toSeparator);
					}
					// Add a new line and process next line
					buffer.append("\n");
				}
			}
			output.writeBytes(buffer.toString());

		} catch (IOException e) {
			log.error("Could not convert file ", e);
			return false;
		} finally {
			try {
				input.close();
			} catch (IOException e) { /* ignore */
			}
			try {
				output.close();
			} catch (IOException e) { /* ignore */
			}
		}
		return true;
	}

	/**
	 * 
	 * @param filename
	 * @param extension
	 * @return
	 */
	public static String changeExtension(String filename, String extension) {
		File file = null;
		try {
			file = new File(filename);
			// file.createNewFile(); // if one does not already exist

			// Get the filename with the new extension
			StringBuffer buffer = new StringBuffer();
			buffer.append(file.getParent()).append(File.separator)
					.append(removeExtension(file.getName())).append(".")
					.append(extension.replace(".", ""));

			file = new File(buffer.toString());

		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return file.getAbsolutePath();

	}

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public static String removeExtension(String filename) {
		int lastPeriod = filename.lastIndexOf(".");
		if (lastPeriod > 0) {
			return filename.substring(0, lastPeriod);
		}
		return filename;

	}

	public static int calculateYearsBetween(Date fromDate, Date toDate) {

		Calendar from = Calendar.getInstance();
		from.setTime(fromDate);

		Calendar to = Calendar.getInstance();
		to.setTime(toDate);

		int yearsBetween = from.get(Calendar.YEAR) - to.get(Calendar.YEAR);
		if (to.get(Calendar.DAY_OF_YEAR) > from.get(Calendar.DAY_OF_YEAR)) {
			yearsBetween -= 1;
		}
		return yearsBetween;
	}

	public static List<PatientIdentifierType> getPatientIdentifierTypesFromGlobalProperty() {
		List<PatientIdentifierType> ret = new ArrayList<PatientIdentifierType>();
		String gp = Context.getAdministrationService().getGlobalProperty(
				"formdataexport.patientIdentifierTypes");
		if (gp == null || gp.equals(""))
			throw new RuntimeException(
					"You must set a value for the global property formdataexport.patientIdentifierTypes");
		for (StringTokenizer st = new StringTokenizer(gp, ","); st
				.hasMoreTokens();) {
			String s = st.nextToken().trim();
			PatientIdentifierType pit = Context.getPatientService()
					.getPatientIdentifierTypeByName(s);
			if (pit == null)
				throw new RuntimeException(
						"The patient identifier type "
								+ s
								+ " can't be found in this system's patient identifier types.");
			else
				ret.add(pit);
		}

		return ret;
	}

	/**
	 * Returns the list of person attribute types configured in the
	 * formdataexport.personAttributeTypes global property.
	 * 
	 * @return The list of person attribute types or null if the global property
	 *         is empty
	 */
	public static List<String> getPersonAttributeTypesFromGlobalProperty() {
		List<String> ret = new ArrayList<String>();
		String gp = Context.getAdministrationService().getGlobalProperty(
				"formdataexport.personAttributeTypes");

		if (gp == null || gp.equals("")) {
			return null;
		}

		for (StringTokenizer st = new StringTokenizer(gp,
				GLOBAL_PROPERTY_DEFAULT_SEPARATOR); st.hasMoreTokens();) {
			String s = st.nextToken().trim();

			StringTokenizer titleValue = new StringTokenizer(s,
					GLOBAL_PROPERTY_TITLE_SEPARATOR);

			titleValue.nextToken(); // Read past title

			String pat = titleValue.nextToken();

			ret.add(pat);
		}

		return ret;
	}

	/**
	 * Returns the list of person attribute titles configured in the
	 * formdataexport.personAttributeTypes global property.
	 * 
	 * @return The list of person attribute titles or null if the global
	 *         property is empty
	 */
	public static List<String> getPersonAttributeTitlesFromGlobalProperty() {
		List<String> ret = new ArrayList<String>();
		String gp = Context.getAdministrationService().getGlobalProperty(
				"formdataexport.personAttributeTypes");

		if (gp == null || gp.equals("")) {
			return null;
		}

		for (StringTokenizer st = new StringTokenizer(gp,
				GLOBAL_PROPERTY_DEFAULT_SEPARATOR); st.hasMoreTokens();) {
			String s = st.nextToken().trim();

			StringTokenizer titleValue = new StringTokenizer(s,
					GLOBAL_PROPERTY_TITLE_SEPARATOR);

			ret.add(titleValue.nextToken());
		}

		return ret;
	}

	/**
	 * Returns the list of person address columns configured in the
	 * formdataexport.personAddressColumns global property.
	 * 
	 * @return The list of person address columns or null if the global property
	 *         is empty
	 */
	public static List<String> getPersonAddressColumnsFromGlobalProperty() {
		List<String> ret = new ArrayList<String>();
		String gp = Context.getAdministrationService().getGlobalProperty(
				"formdataexport.personAddressColumns");

		if (gp == null || gp.equals("")) {
			return null;
		}

		for (StringTokenizer st = new StringTokenizer(gp,
				GLOBAL_PROPERTY_DEFAULT_SEPARATOR); st.hasMoreTokens();) {
			String s = st.nextToken().trim();

			StringTokenizer titleValue = new StringTokenizer(s,
					GLOBAL_PROPERTY_TITLE_SEPARATOR);

			titleValue.nextToken(); // Read past title

			ret.add(titleValue.nextToken());
		}

		return ret;
	}

	/**
	 * Returns the list of person address column titles configured in the global
	 * formdataexport.personAddressColumns global property.
	 * 
	 * @return The list of person address column titles or null if the global
	 *         property is empty
	 */
	public static List<String> getPersonAddressColumnTitlesFromGlobalProperty() {
		List<String> ret = new ArrayList<String>();
		String gp = Context.getAdministrationService().getGlobalProperty(
				"formdataexport.personAddressColumns");

		if (gp == null || gp.equals("")) {
			return null;
		}

		for (StringTokenizer st = new StringTokenizer(gp,
				GLOBAL_PROPERTY_DEFAULT_SEPARATOR); st.hasMoreTokens();) {
			String s = st.nextToken().trim();

			StringTokenizer titleValue = new StringTokenizer(s,
					GLOBAL_PROPERTY_TITLE_SEPARATOR);

			ret.add(titleValue.nextToken());
		}

		return ret;
	}

	/**
	 * Returns the list of column suffixes to skip from the
	 * formdataexport.skipColumnsWithSuffix global property.
	 * 
	 * @return The list of column suffixes to skip or null if the global
	 *         property is empty
	 */
	public static List<String> getColumnSuffixesToSkipFromGlobalProperty() {
		List<String> ret = new ArrayList<String>();
		String gp = Context.getAdministrationService().getGlobalProperty(
				"formdataexport.skipColumnsWithSuffix");

		if (gp == null || gp.equals("")) {
			return null;
		}

		StringTokenizer st = new StringTokenizer(gp,
				GLOBAL_PROPERTY_EXCLUSION_SEPARATOR);

		String exclusions = st.nextToken();

		StringTokenizer exclusionTokens = new StringTokenizer(exclusions,
				GLOBAL_PROPERTY_DEFAULT_SEPARATOR);

		while (exclusionTokens.hasMoreTokens()) {
			ret.add(exclusionTokens.nextToken());
		}

		return ret;
	}

	/**
	 * Returns the list of columns to exclude from column suffix skipping from
	 * the formdataexport.skipColumnsWithSuffix global property.
	 * 
	 * @return The list of columns to exclude or null if the global property is
	 *         empty
	 */
	public static List<String> getColumnSuffixSkipExclusionsFromGlobalProperty() {
		List<String> ret = new ArrayList<String>();
		String gp = Context.getAdministrationService().getGlobalProperty(
				"formdataexport.skipColumnsWithSuffix");

		if (gp == null || gp.equals("")) {
			return null;
		}

		StringTokenizer st = new StringTokenizer(gp,
				GLOBAL_PROPERTY_EXCLUSION_SEPARATOR);

		st.nextToken(); // Read past exclusions

		if (!st.hasMoreTokens()) {
			return null;
		}

		String exclusions = st.nextToken();

		StringTokenizer exclusionTokens = new StringTokenizer(exclusions,
				GLOBAL_PROPERTY_DEFAULT_SEPARATOR);

		while (exclusionTokens.hasMoreTokens()) {
			ret.add(exclusionTokens.nextToken());
		}

		return ret;
	}

	/**
	 * Gets the value of a person attribute for a patient
	 * 
	 * @param personAttributeColumn
	 *            The attribute value to get
	 * @param patient
	 *            The patient
	 * @return The attribute value or an empty string if the patient has no
	 *         value for the attribute
	 */
	public static String getPersonAttributeValue(String personAttributeColumn,
			Patient patient) {

		if (personAttributeColumn.equals(SPECIAL_PERSON_ATTRIBUTE_AGE)) {
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_OF_BIRTH_FORMAT);
			String birthdate = "";

			if (patient.isBirthdateEstimated()) {
				birthdate = Context.getMessageSourceService().getMessage(
						"Person.birthdateEstimated");
			} else {
				birthdate = sdf.format(patient.getBirthdate());
			}

			return patient.getAge() + " (" + birthdate + ")";
		} else if (personAttributeColumn
				.equals(SPECIAL_PERSON_ATTRIBUTE_GENDER)) {
			return patient.getGender();
		} else if (personAttributeColumn
				.equals(SPECIAL_PERSON_ATTRIBUTE_GIVEN_NAME)) {
			return patient.getGivenName();
		} else if (personAttributeColumn
				.equals(SPECIAL_PERSON_ATTRIBUTE_FAMILY_NAME)) {
			return patient.getFamilyName();
		} else {
			try {
				PersonAttribute pa = patient.getAttribute(Context
						.getPersonService().getPersonAttributeTypeByName(
								personAttributeColumn));
				if (pa != null) {
					return pa.toString();
				} else {
					return "";
				}
			} catch (Exception e) {
				log.error("Error adding patient attribute "
						+ personAttributeColumn + ": " + e.getMessage());
				return "";
			}
		}
	}

	/**
	 * Get the value for a patient address column
	 * 
	 * @param personAddressColumn
	 *            The column to get the value for
	 * @param patient
	 *            The patient
	 * @return The value of the address column or an empty string if the patient
	 *         doesn't have a value for the address column
	 */
	public static String getPersonAddressValue(String personAddressColumn,
			Patient patient) {

		// Get the preffered address
		PersonAddress address = getPatientPreferredAddress(patient);

		if (address == null) {
			return "";
		} else if (!ADDRESS_COLUMNS.contains(personAddressColumn)) {
			log.error("Address column " + personAddressColumn + " not found.");
		} else {
			try {
				Method method = address.getClass().getMethod(
						"get" + StringUtils.capitalize(personAddressColumn));
				String ret = (String) method.invoke(address);

				if (ret == null) {
					return "";
				} else {
					return ret;
				}

			} catch (Exception e) {
				log.error("Error adding address column for "
						+ personAddressColumn + ": " + e.getMessage());
				return "";
			}
		}

		return "";
	}

	/**
	 * Find the preferred address of the patient
	 * 
	 * @param patient
	 * @return The preferred address or the first one if none are preferred
	 */
	public static PersonAddress getPatientPreferredAddress(Patient patient) {
		Set<PersonAddress> addresses = patient.getAddresses();
		PersonAddress ret = null;

		// If there is only one address, use it
		if (addresses.size() == 1) {
			return addresses.iterator().next();
		}

		// Attempt to find the preferred address
		for (PersonAddress address : addresses) {
			if (address.getPreferred()) {
				ret = address;
			}
		}

		// Use the first one if none is prefferred.
		if ((addresses.size() > 0) && (ret == null)) {
			return addresses.iterator().next();
		}

		return ret;
	}

	/**
	 * Determines whether or not a column should be added
	 * 
	 * @param column
	 *            The column to check
	 * @param exclusionColumnSuffixes
	 *            The suffixes to ignore
	 * @param exclusionExceptions
	 *            The list of exceptions
	 * @return True if the column should be added, false otherwise
	 */
	public static boolean shouldAddColumn(String column,
			List<String> exclusionColumnSuffixes,
			List<String> exclusionExceptions) {
		String columnTitle = column.replace(DEFAULT_QUOTE, ""); // Remove quotes

		if (exclusionColumnSuffixes == null) {
			return true;
		}

		for (String exclusionColumnSuffix : exclusionColumnSuffixes) {
			if ((columnTitle.endsWith(exclusionColumnSuffix))
					&& ((exclusionExceptions == null) || !(exclusionExceptions
							.contains(columnTitle)))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Replace the CSV separator when it exists inside elements in the list
	 * 
	 * @param row
	 *            The string to replace
	 * @return The string with the separator values replaced
	 */
	public static String fixCommasInCSVValues(String row) {
		String ret = "";

		// Replace "," with |||
		ret = row.replaceAll(Pattern.quote(COLUMN_NAME_SEPARATOR_REGEX),
				ROBUST_SEPARATOR);

		// Replace , with :
		ret = ret.replace(GLOBAL_PROPERTY_DEFAULT_SEPARATOR,
				CSV_VALUE_COMMA_REPLACEMENT);

		// Replace ||| with ","
		ret = ret.replaceAll(Pattern.quote(ROBUST_SEPARATOR),
				COLUMN_NAME_SEPARATOR_REGEX);

		return ret;
	}
}
