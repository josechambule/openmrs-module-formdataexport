<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<h2>
	<spring:message code="formdataexport.countForms.title" />
</h2>

<form method="post">
	<spring:message code="formdataexport.countForms.whichProgram"/>:
	<spring:bind path="command.program">
		<select name="${status.expression}">
			<option value=""><spring:message code="formdataexport.countForms.all"/></option>
			<openmrs:forEachRecord name="workflowProgram">
				<option value="${record.programId}">${record.concept.name}</option>
			</openmrs:forEachRecord>
		</select>
	</spring:bind>
	<input type="submit" value="<spring:message code="formdataexport.countForms.title"/>"/>
</form>

<c:if test="${not empty command.results}">
	<hr/>
	<h4>
		<c:if test="${empty command.program}">
			<spring:message code="formdataexport.countForms.results"/>
		</c:if>
		<c:if test="${not empty command.program}">
			<spring:message code="formdataexport.countForms.resultsForProgram" arguments="${command.program.concept.name}"/>
		</c:if>
	</h4>
	<table>
		<c:forEach var="e" items="${command.results}">
			<tr>
				<td>${e.key.name}</td>
				<td>${e.value}</td>
			</tr>
		</c:forEach>
	</table>
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp"%>
