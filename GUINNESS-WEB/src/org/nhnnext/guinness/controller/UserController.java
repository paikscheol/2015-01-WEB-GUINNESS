package org.nhnnext.guinness.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolation;

import org.nhnnext.guinness.dao.ConfirmDao;
import org.nhnnext.guinness.dao.UserDao;
import org.nhnnext.guinness.exception.AlreadyExistedUserIdException;
import org.nhnnext.guinness.exception.UserUpdateException;
import org.nhnnext.guinness.model.User;
import org.nhnnext.guinness.util.MyValidatorFactory;
import org.nhnnext.guinness.util.RandomFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UserController {
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserDao userDao;

	@Autowired
	private ConfirmDao confirmDao;

	@Autowired
	private JavaMailSender mailSender;

	@RequestMapping("/")
	protected String init(Model model) {
		model.addAttribute("user", new User());
		return "index";
	}

	@RequestMapping(value = "/user", method = RequestMethod.POST)
	protected String create(Model model, User user) throws AlreadyExistedUserIdException, MessagingException {
		logger.debug("User: {}", user);
		if (!extractViolationMessage(model, user)) {
			return "index";
		}
		
		//  User 객체 받아오기
		User existedUser = userDao.findUserByUserId(user.getUserId());

		// 활성화된 계정일 경우
		if(existedUser == null)
			userDao.createUser(user);
		
		if(existedUser.getStatus() == 'E')
			throw new AlreadyExistedUserIdException();

		if(existedUser.getStatus() == 'R') {
			confirmDao.deleteConfirmByUserId(user.getUserId());
		}
		
		String keyAddress = RandomFactory.getRandomId(10);
		sendMail(keyAddress, user.getUserId());
		confirmDao.createConfirm(keyAddress, user.getUserId());
		logger.debug("user create success");
		return "sendEmail";
	}

	@RequestMapping(value = "/user/confirm/{keyAddress}")
	protected String confirm(@PathVariable String keyAddress, HttpSession session) throws IOException {
		String userId = confirmDao.findUserIdByKeyAddress(keyAddress);
		userDao.updateUserState(userId, 'E');
		confirmDao.deleteConfirmByKeyAddress(keyAddress);
		User user = userDao.findUserByUserId(userId);
		session.setAttribute("sessionUserId", userId);
		session.setAttribute("sessionUserName", user.getUserName());
		session.setAttribute("sessionUserImage", user.getUserImage());
		return "redirect:/";
	}

	public void sendMail(String keyAddress, String userId) throws MessagingException, NullPointerException {
		logger.debug("sendMail");
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");
		messageHelper.setTo(userId);
		messageHelper.setFrom("hakimaru@naver.com");
		messageHelper.setSubject("환영합니다. 페이퍼민트 가입 인증 메일입니다."); // 메일제목은 생략이 가능하다
		messageHelper.setText("<a href='http://localhost:8080/user/confirm/" + keyAddress + "'> 가입하기 </a>", true);
		mailSender.send(message);
		logger.debug("Mail is sended");
	}

	@RequestMapping(value = "/user")
	protected String updateForm(Model model) {
		model.addAttribute("user", new User());
		return "updateUser";
	}

	@RequestMapping(value = "/user/update", method = RequestMethod.POST)
	protected String updateUser(WebRequest req, HttpSession session, Model model, User user, @RequestParam("profileImage") MultipartFile profileImage) throws UserUpdateException, IllegalStateException, IOException {

		// TODO 추후 password 입력 양식 변경 시 리펙토링 필요
		String userNewPassword = req.getParameter("userNewPassword");
		User prevUser = userDao.findUserByUserId(user.getUserId());
		
		user.setUserImage(prevUser.getUserImage());
		if (!profileImage.isEmpty()) {
			logger.debug("Img change");
			String fileName = user.getUserId();
			String rootPath = session.getServletContext().getRealPath("/");
			profileImage.transferTo(new File(rootPath+"img/profile/" + fileName));
			user.setUserImage(fileName);
		}

		String userPassword = user.getUserPassword();
		if (userNewPassword.equals(""))
			userNewPassword = userPassword;
		user.setUserPassword(userNewPassword);

		if (!extractViolationMessage(model, user))
			throw new UserUpdateException("잘못된 형식입니다.");

		if (!prevUser.getUserPassword().equals(userPassword)) {
			throw new UserUpdateException("비밀번호가 일치하지 않습니다.");
		}

		try {
			userDao.updateUser(user);
		} catch (DataIntegrityViolationException e) {
			throw new UserUpdateException("잘못된 형식입니다.");
		}
		
		//session refresh...
		session.setAttribute("sessionUserName", user.getUserName());
		session.setAttribute("sessionUserImage", user.getUserImage());
		return "redirect:/groups";
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	protected ModelAndView login(WebRequest req, HttpSession session) throws IOException {
		String userId = req.getParameter("userId");
		String userPassword = req.getParameter("userPassword");
		Map<String, String> viewMap = new HashMap<String, String>();

		User user = userDao.findUserByUserId(userId);
		if (user == null || !user.getUserPassword().equals(userPassword) || user.getStatus() != 'E') {
			viewMap.put("view", "loginFailed");
			return new ModelAndView("jsonView").addObject("jsonData", viewMap);
		}
		session.setAttribute("sessionUserId", user.getUserId());
		session.setAttribute("sessionUserName", user.getUserName());
		session.setAttribute("sessionUserImage", user.getUserImage());
		viewMap.put("view", "groups");
		return new ModelAndView("jsonView").addObject("jsonData", viewMap);
	}

	@RequestMapping("/logout")
	protected String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/";
	}

	private boolean extractViolationMessage(Model model, User user) {
		Set<ConstraintViolation<User>> constraintViolations = MyValidatorFactory.createValidator().validate(user);
		if (!constraintViolations.isEmpty()) {
			String signValidErrorMessage = "";
			Iterator<ConstraintViolation<User>> violations = constraintViolations.iterator();
			while (violations.hasNext()) {
				ConstraintViolation<User> each = violations.next();
				signValidErrorMessage = signValidErrorMessage + "<br />" + each.getMessage();
			}
			model.addAttribute("signValidErrorMessage", signValidErrorMessage);
			logger.debug("violation error");
			return false;
		}
		return true;
	}
}
