package org.openmrs.module.formdataexport.db.hibernate;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.db.LoginCredential;
import org.openmrs.module.formdataexport.db.UserDataExportDAO;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author jose.chambule
 *
 */
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
		List<User> users = query.list();

		if (users == null || users.size() == 0) {
			log.warn("request for username '" + username + "' not found");
			return null;
		}

		return users;
	}

	@Override
	public LoginCredential getUserLoginCredential(User user) {
		// TODO Auto-generated method stub
		return (LoginCredential) sessionFactory.getCurrentSession().get(LoginCredential.class, user.getUserId());
	}

	@Override
	public void importUser(User user, LoginCredential credentials) {
		// TODO Auto-generated method stub
		user.setUserId(null);
		sessionFactory.getCurrentSession().saveOrUpdate(user);
		updateUserPassword(user, credentials);
	}
	
	private void updateUserPassword(User user, LoginCredential credentials) {
		User changeForUser = getUser(user.getUserId());
		if (changeForUser == null) {
			throw new DAOException("Couldn't find user to set password for userId=" + user.getUserId());
		}
		User changedByUser = getUser(Context.getAuthenticatedUser().getUserId());
		LoginCredential credential = getLoginCredential(changeForUser);
		credential.setUserId(user.getUserId());
		credential.setHashedPassword(credentials.getHashedPassword());
		credential.setSalt(credentials.getSalt());
		credential.setChangedBy(changedByUser);
		credential.setDateChanged(new Date());
		credential.setUuid(changeForUser.getUuid());
		credential.setSecretQuestion(credentials.getSecretQuestion());
		credential.setSecretAnswer(credentials.getSecretAnswer());
		
		sessionFactory.getCurrentSession().merge(credential);
		
		// reset lockout 
		changeForUser.setUserProperty(OpenmrsConstants.USER_PROPERTY_LOCKOUT_TIMESTAMP, "");
		changeForUser.setUserProperty(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS, "0");
		sessionFactory.getCurrentSession().saveOrUpdate(changeForUser);
	}
	
	public User getUser(Integer userId) {
		return (User) sessionFactory.getCurrentSession().get(User.class, userId);
	}
	
	public LoginCredential getLoginCredential(User user) {
		return (LoginCredential) sessionFactory.getCurrentSession().get(LoginCredential.class, user.getUserId());
	}

}
