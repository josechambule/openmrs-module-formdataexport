package org.openmrs.module.formdataexport.db.hibernate;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.openmrs.User;
import org.openmrs.module.formdataexport.db.UserDataExportDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("formdataexport.HibernateUserDataExportDAO")
public class HibernateUserDataExportDAO implements UserDataExportDAO {

	protected final Log log = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<User> getUserByUsername(String username) {
		// TODO Auto-generated method stub
		Query query = sessionFactory.getCurrentSession()
				.createQuery("from User u where u.username like '%" + username + "%'");
		// query.setString("username", "%"+username+"%");
		// query.setString("username", "%"+username+"%");
		List<User> users = query.list();

		if (users == null || users.size() == 0) {
			log.warn("request for username '" + username + "' not found");
			return null;
		}

		return users;
	}

}
