<ul id="menu">
	<li class="first">
		<a href="${pageContext.request.contextPath}/admin"><spring:message code="admin.title.short"/></a>
	</li>
	<openmrs:hasPrivilege privilege="Manage Form Exports">
		<li <c:if test="<%= request.getRequestURI().contains("formdataexport/formDataExportList") %>">class="active"</c:if>>
			<a href="${pageContext.request.contextPath}/module/formdataexport/formDataExport.list">
				<spring:message code="formdataexport.FormDataExport.manage"/>
			</a>
		</li>
	</openmrs:hasPrivilege>

</ul>