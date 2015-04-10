package org.nhnnext.guinness.controller.groups;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nhnnext.guinness.exception.MakingObjectListFromJdbcException;
import org.nhnnext.guinness.exception.SessionUserIdNotFoundException;
import org.nhnnext.guinness.model.Group;
import org.nhnnext.guinness.model.GroupDao;
import org.nhnnext.guinness.util.Forwarding;
import org.nhnnext.guinness.util.ServletRequestUtil;
import org.nhnnext.guinness.util.WebServletUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@WebServlet(WebServletUrl.GROUP_READ)
public class ReadGroupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(ReadGroupServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		List<Group> groupList = null;
		try {
			String sessionUserId = ServletRequestUtil.checkSessionAttribute(req, resp);
			groupList = GroupDao.getInstance().readGroupList(sessionUserId);
		} catch (SQLException | ClassNotFoundException | MakingObjectListFromJdbcException e) {
			logger.error(e.getClass().getSimpleName() + "에서 exception 발생", e);
			Forwarding.forwardForException(req, resp);
			return;
		} catch (SessionUserIdNotFoundException e) {
			return;
		}
		resp.setContentType("application/json; charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.write(new Gson().toJson(groupList));
		out.close();
	}
}
