package org.openmrs.module.formdataexport.db;

import java.util.List;

import org.openmrs.User;
import org.openmrs.api.db.LoginCredential;

public interface UserDataExportDAO {
	public List<User> getUserByUsername(String username);
	public LoginCredential getUserLoginCredential(User user);
}
