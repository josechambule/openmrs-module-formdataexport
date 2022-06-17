package org.openmrs.module.formdataexport.db.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Cohort;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.module.formdataexport.db.FormDataExportDAO;

public class HibernateFormDataExportDAO implements FormDataExportDAO {

	private SessionFactory sessionFactory;
	
	protected final Log log = LogFactory.getLog(getClass());
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public Map<Form, Integer> countForms(Cohort cohort) {
		String q = "select e.form, count(*) from Encounter e";
		if (cohort != null)
			q += " where e.patient.personId in (:cohortSet)";
		q += " group by e.form";
		Query query = sessionFactory.getCurrentSession().createQuery(q);
		if (cohort != null)
			query.setParameterList("cohortSet", cohort.getMemberIds());
		
		
		
		List<Object[]> l = new ArrayList<Object[]>(query.list());
		Collections.sort(l, new Comparator<Object[]>() {
				public int compare(Object[] left, Object[] right) {
					return ((Form) left[0]).getName().compareTo(((Form) right[0]).getName());
				}
			});
		Map<Form, Integer> ret = new LinkedHashMap<Form, Integer>();
		for (Object[] o : l)
			ret.put((Form) o[0], ((Number) o[1]).intValue());
		return ret;
	}
	
	/**
	 * Gets a list of encounters associated with the given form, filtered by the given patient set.
	 * 
	 * @param	patients	the patients to filter by (null will return all encounters for all patients)
	 * @param 	forms		the forms to filter by
	 */
	@SuppressWarnings("unchecked")
	public List<Encounter> getEncountersByForm(Cohort cohort, List<Form> forms) {
		return getEncountersByForm(cohort, forms, null, null, null);
	}	
	
	public List<Encounter> getEncountersByForm(Cohort cohort, List<Form> forms, Date startDate, Date endDate, String firstLast){
	    // default query
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Encounter.class);
        criteria.setCacheMode(CacheMode.IGNORE);
        
        // this "where clause" is only necessary if patients were passed in
        if (cohort != null && cohort.size() > 0)
            criteria.add(Restrictions.in("patient.personId", cohort.getMemberIds()));
        
        criteria.add(Restrictions.eq("voided", false));
        
        if (forms != null && forms.size() > 0)
            criteria.add(Restrictions.in("form", forms));
        
        if (startDate != null)
            criteria.add(Expression.ge("encounterDatetime", startDate));
        if (endDate != null)
            criteria.add(Expression.le("encounterDatetime", endDate));
        criteria.addOrder(org.hibernate.criterion.Order.asc("patient.personId"));
        if (firstLast != null && firstLast.equals("last"))
            criteria.addOrder(org.hibernate.criterion.Order.desc("encounterDatetime"));
        else 
            criteria.addOrder(org.hibernate.criterion.Order.asc("encounterDatetime"));
        return criteria.list();
	}
	
	public Cohort getPatientsHavingEncounters(List<EncounterType> encounterTypeList, Location location, Form form,
	        Date fromDate, Date toDate, Integer minCount, Integer maxCount) {
		List<Integer> encTypeIds = null;
		if (encounterTypeList != null && encounterTypeList.size() > 0) {
			encTypeIds = new ArrayList<Integer>();
			for (EncounterType t : encounterTypeList)
				encTypeIds.add(t.getEncounterTypeId());
		}
		Integer locationId = location == null ? null : location.getLocationId();
		Integer formId = form == null ? null : form.getFormId();
		List<String> whereClauses = new ArrayList<String>();
		whereClauses.add("e.voided = false");
		if (encTypeIds != null)
			whereClauses.add("e.encounter_type in (:encTypeIds)");
		if (locationId != null)
			whereClauses.add("e.location_id = :locationId");
		if (formId != null)
			whereClauses.add("e.form_id = :formId");
		if (fromDate != null)
			whereClauses.add("e.encounter_datetime >= :fromDate");
		if (toDate != null)
			whereClauses.add("e.encounter_datetime <= :toDate");
		List<String> havingClauses = new ArrayList<String>();
		if (minCount != null)
			havingClauses.add("count(*) >= :minCount");
		if (maxCount != null)
			havingClauses.add("count(*) >= :maxCount");
		StringBuilder sb = new StringBuilder();
		sb.append(" select e.patient_id from encounter e ");
		sb.append(" inner join patient p on e.patient_id = p.patient_id and p.voided = false ");
		for (ListIterator<String> i = whereClauses.listIterator(); i.hasNext();) {
			sb.append(i.nextIndex() == 0 ? " where " : " and ");
			sb.append(i.next());
		}
		sb.append(" group by e.patient_id ");
		for (ListIterator<String> i = havingClauses.listIterator(); i.hasNext();) {
			sb.append(i.nextIndex() == 0 ? " having " : " and ");
			sb.append(i.next());
		}
		log.debug("query: " + sb);
		
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sb.toString());
		if (encTypeIds != null)
			query.setParameterList("encTypeIds", encTypeIds);
		if (locationId != null)
			query.setInteger("locationId", locationId);
		if (formId != null)
			query.setInteger("formId", formId);
		if (fromDate != null)
			query.setDate("fromDate", fromDate);
		if (toDate != null)
			query.setDate("toDate", toDate);
		if (minCount != null)
			query.setInteger("minCount", minCount);
		if (maxCount != null)
			query.setInteger("maxCount", maxCount);
		
		return new Cohort(query.list());
	}
}
