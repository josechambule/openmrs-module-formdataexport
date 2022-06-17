package org.openmrs.module.formdataexport.db;

import java.util.List;

import org.openmrs.User;

public interface UserDataExportDAO {
	public List<User> getUserByUsername(String username);
}
