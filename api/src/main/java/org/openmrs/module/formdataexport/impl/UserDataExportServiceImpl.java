package org.openmrs.module.formdataexport.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.db.LoginCredential;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.formdataexport.UserDataExportService;
import org.openmrs.module.formdataexport.db.UserDataExportDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author jose.chambule
 *
 */
@Service(UserDataExportServiceImpl.BEAN_NAME)
public class UserDataExportServiceImpl extends BaseOpenmrsService implements UserDataExportService {

	protected final Log log = LogFactory.getLog(UserDataExportServiceImpl.class);
	
	public static final String BEAN_NAME = "formdataexport.userDataExportService";
	
	protected UserDataExportDAO dao;
	
	public UserDataExportServiceImpl() {
	}
	
	@Autowired
	public void setUserDataExportDAO(UserDataExportDAO dao) {
		this.dao = dao;
	}

	@Override
	public List<User> getUserByUsername(String username) throws APIException {
		// TODO Auto-generated method stub
		return dao.getUserByUsername(username);
	}
	
	@Override
	public LoginCredential getUserLoginCredential(User user) {
		// TODO Auto-generated method stub
		return dao.getUserLoginCredential(user);
	}

	@Override
	public void importUser(User user, LoginCredential credentials) {
		// TODO Auto-generated method stub
		dao.importUser(user, credentials);
	}

}
