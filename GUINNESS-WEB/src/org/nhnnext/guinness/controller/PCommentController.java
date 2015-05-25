package org.nhnnext.guinness.controller;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.nhnnext.guinness.exception.UnpermittedAccessGroupException;
import org.nhnnext.guinness.model.Note;
import org.nhnnext.guinness.model.PComment;
import org.nhnnext.guinness.model.SessionUser;
import org.nhnnext.guinness.service.PCommentService;
import org.nhnnext.guinness.util.JsonResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/pComments")
public class PCommentController {
	@Resource
	private PCommentService pCommentService;

	@RequestMapping(value = "", method = RequestMethod.POST)
	protected @ResponseBody JsonResult create(HttpSession session, @RequestParam String pId,
			@RequestParam String sameSenCount, @RequestParam String sameSenIndex, @RequestParam String pCommentText,
			@RequestParam String selectedText, @RequestParam String noteId) throws IOException {
		SessionUser sessionUser = (SessionUser) session.getAttribute("sessionUser");
		Note note = new Note(noteId);
		if (pCommentText.equals(""))
			return new JsonResult().setSuccess(false);

		PComment pComment = new PComment(pId, sameSenCount, sameSenIndex, pCommentText, selectedText, sessionUser, note);

		try {
			return new JsonResult().setSuccess(true).setMapValues(pCommentService.create(sessionUser, note, pComment));
		} catch (UnpermittedAccessGroupException e) {
			return new JsonResult().setSuccess(false).setMessage(e.getMessage()).setLocationWhenFail("/illegal");
		}
	}

	@RequestMapping("/{noteId}")
	protected @ResponseBody JsonResult list(@PathVariable String noteId) {
		return new JsonResult().setSuccess(true).setMapValues(pCommentService.list(noteId));
	}

	@RequestMapping(value = "/{pCommentId}", method = RequestMethod.PUT)
	protected @ResponseBody JsonResult update(@PathVariable String commentId, @RequestParam String commentText) {
		return new JsonResult().setSuccess(true).setObject((PComment) pCommentService.update(commentId, commentText));
	}

	@RequestMapping(value = "/{pCommentId}", method = RequestMethod.DELETE)
	protected @ResponseBody JsonResult delete(@PathVariable String commentId) {
		pCommentService.delete(commentId);
		return new JsonResult().setSuccess(true);
	}
}
