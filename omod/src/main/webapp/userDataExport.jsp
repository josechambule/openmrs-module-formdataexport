<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
<openmrs:htmlInclude file="/scripts/jquery/jquery-1.3.2.min.js" />
<script type="text/javascript">
	var $j = jQuery.noConflict();
</script>
<h2>
	<spring:message code="formdataexport.UserDataExport.title" />
</h2>
<style>
.styled-table {
	border-collapse: collapse;
	margin: 25px 0;
	font-size: 0.9em;
	font-family: sans-serif;
	min-width: 400px;
	box-shadow: 0 0 20px rgba(0, 0, 0, 0.15);
}

.styled-table thead tr {
	background-color: #009879;
	color: #ffffff;
	text-align: left;
}

.styled-table th, .styled-table td {
	padding: 12px 15px;
}

.styled-table tbody tr {
	border-bottom: 1px solid #dddddd;
}

.styled-table tbody tr:nth-of-type(even) {
	background-color: #f3f3f3;
}

.styled-table tbody tr:last-of-type {
	border-bottom: 2px solid #009879;
}

.styled-table tbody tr.active-row {
	font-weight: bold;
	color: #009879;
}

.button {
	border: none;
	color: white;
	padding: 15px 32px;
	text-align: center;
	text-decoration: none;
	display: inline-block;
	font-size: 16px;
	margin: 4px 2px;
	cursor: pointer;
	background-color: #4CAF50;
}
</style>
<script type="text/javascript">

	function search() {
		var searchByName = document.getElementById("searchId");
		window.location.assign("userDataExport.list?searchId=" + searchByName);
	}
		
	function addUserId(pageId,recordsPerPage) {
		var cboxes = document.getElementsByName('checkButton');
	    var len = cboxes.length;
	    var index = "";
	    for (var i=0; i<len; i++) {
	    	if(cboxes[i].checked) {
	    		index = index + cboxes[i].value + "a";
	    	}	        
	    }
	    
	    var pesquisaId = document.getElementById('searchForm').elements['searchId'].value;
	    
	    if(index!="") {
	    	window.location.assign("userDataExport.list?page="+ pageId + "&recordsPerPage=" + recordsPerPage + "&searchId=" + pesquisaId + "&userIDList=" + index);
	    }else {
	    	window.location.assign("userDataExport.list?page="+ pageId + "&recordsPerPage=" + recordsPerPage + "&searchId=" + pesquisaId);
	    }
	}
	
</script>
<b class="boxHeader">Select User</b>
<div class="box">
	<form action="" method="get" name="searchForm" id="searchForm">
		<div class="row" align="left">
			<table style="width: 100%;" cellpadding="5" cellspacing="5">
				<tr>
					<td nowrap><spring:message
							code="formdataexport.UserDataExport.filterUser" /></td>
					<td><spring:message
							code="formdataexport.UserDataExport.searchUser" /><input
						type="text" name="searchId" id="searchId" value="${searchId}" />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input
						type="submit" value='Search' onclick="search()" /></td>
				</tr>
			</table>
		</div>
	</form>
	<form method="post">
		
		<div class="row" align="left">
			<table class="styled-table" style="width: 99%; margin-left: 5px;"
				cellpadding="5" cellspacing="5">
				<thead>
					<tr>
						<th>Select User</th>
						<th>User Id</th>
						<th>System Id</th>
						<th>User Name</th>
						<th>User UUID</th>
						<th>Person Id</th>
						<th>Person Name</th>
						<th>Family Name</th>
						<th>Middle Name</th>
						<th>Person UUID</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="user" items="${userList}">
						<tr>
							<td><input type="checkbox" name="checkButton" value="${user.userId}"></td>
							<td>${user.userId}</td>
							<td>${user.systemId}</td>
							<td>${user.username}</td>
							<td>${user.uuid}</td>
							<td>${user.person.personId}</td>
							<td>${user.person.givenName}</td>
							<td>${user.person.familyName}</td>
							<td>${user.person.middleName}</td>
							<td>${user.person.uuid}</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
			<div class="row" align="right">
				<table style="margin-right: 1%;" cellpadding="5" cellspacing="5">
					<tr>
						<td><spring:message
								code="formdataexport.UserDataExport.recordsPerPage" /> <select
							name="recordsPerPage"
							onChange="window.location = 'userDataExport.list?recordsPerPage=' + this.options[this.selectedIndex].value;"
							id="recordsPerPage">
								<option value="${recordsPerPage}" SELECTED>${recordsPerPage}</option>
								<c:forEach var="record" items="${recordsPerPageList}">
									<option value="${record.value}">${record.value}</option>
								</c:forEach>
						</select></td>
						<%--For displaying Previous link except for the 1st page --%>
						<c:if test="${currentPage != 1}">
							<td><a href="#" onclick="addUserId(${1},${recordsPerPage})">First</a></td>
						</c:if>
						<c:if test="${currentPage != 1}">
							<td><a href="#" onclick="addUserId(${currentPage - 1},${recordsPerPage})">Previous</a></td>
						</c:if>

						<%--For displaying Page numbers. 
							    The when condition does not display a link for the current page--%>

						<div class="page-nav">
							<c:choose>
								<c:when test="${noOfPages <= 20}">
									<c:forEach begin="1" end="${noOfPages}" var="i">
										<c:choose>
											<c:when test="${currentPage eq i}">
												<td>${i}</td>
											</c:when>
											<c:otherwise>
												<td><a href="#" onclick="addUserId(${i},${recordsPerPage})">${i}</a></td>
											</c:otherwise>
										</c:choose>
									</c:forEach>
								</c:when>
								<c:when test="${(pointPage + currentPage) >= noOfPages}">
									<c:forEach begin="${noOfPages-20}" end="${noOfPages}" var="i">
										<c:choose>
											<c:when test="${currentPage eq i}">
												<td>${i}</td>
											</c:when>
											<c:otherwise>
												<td><a href="#" onclick="addUserId(${i},${recordsPerPage})">${i}</a></td>
											</c:otherwise>
										</c:choose>
									</c:forEach>
								</c:when>
								<c:otherwise>
									<c:forEach begin="${currentPage}"
										end="${(pointPage + currentPage)}" var="i">
										<c:choose>
											<c:when test="${currentPage eq i}">
												<td>${i}</td>
											</c:when>
											<c:otherwise>
												<td><a href="#" onclick="addUserId(${i},${recordsPerPage})">${i}</a></td>
											</c:otherwise>
										</c:choose>
									</c:forEach>
								</c:otherwise>
							</c:choose>
						</div>

						<%--For displaying Next link --%>
						<c:if test="${currentPage lt noOfPages}">
							<td><a href="#" onclick="addUserId(${currentPage + 1},${recordsPerPage})">Next</a></td>
						</c:if>
						<c:if test="${currentPage lt noOfPages}">
							<td><a href="#" onclick="addUserId(${noOfPages},${recordsPerPage})">Last</a></td>
						</c:if>
					</tr>
				</table>
			</div>
		</div>

		<div class="row" align="left">
			<table style="width: 100%;" cellpadding="5" cellspacing="5">
				<tr>
					<td align="left" colspan="3"><br />&nbsp;&nbsp;&nbsp; 
					<input type="submit" name="export" value='Export User' /></td>
				</tr>
			</table>
		</div>
	</form>
</div>
<%@ include file="/WEB-INF/template/footer.jsp"%>