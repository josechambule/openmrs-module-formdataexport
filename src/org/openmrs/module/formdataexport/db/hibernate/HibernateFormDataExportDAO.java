package org.openmrs.module.formdataexport.db.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Cohort;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.module.formdataexport.db.FormDataExportDAO;

public class HibernateFormDataExportDAO implements FormDataExportDAO {

	private SessionFactory sessionFactory;
	
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
		
		// default query
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Encounter.class);
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// this "where clause" is only necessary if patients were passed in
		if (cohort != null && cohort.size() > 0)
			criteria.add(Restrictions.in("patient.personId", cohort.getMemberIds()));
		
		criteria.add(Restrictions.eq("voided", false));
		
		if (forms != null && forms.size() > 0)
			criteria.add(Restrictions.in("form", forms));
		
		criteria.addOrder(org.hibernate.criterion.Order.desc("patient.personId"));
		criteria.addOrder(org.hibernate.criterion.Order.desc("encounterDatetime"));
		
		return criteria.list();
	
	}		
	

}
