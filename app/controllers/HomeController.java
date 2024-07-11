package controllers;

import models.Login;
import models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.MessagesApi;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.*;

import javax.inject.Inject;

public class HomeController extends Controller {

    Form<User> userForm;
    private final Form<Login> loginForm;
    private final HttpExecutionContext ec;
    private final FormFactory formFactory;
    private final MessagesApi messagesApi;

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public HomeController(HttpExecutionContext ec, FormFactory formFactory, MessagesApi messagesApi) {
        this.loginForm = formFactory.form(Login.class);
        this.ec = ec;
        this.formFactory = formFactory;
        this.userForm = formFactory.form(User.class);
        this.messagesApi = messagesApi;
    }

    public Result index(Http.Request request) {
        return ok(views.html.register.render(userForm, request, messagesApi.preferred(request)));
    }

    public Result login(Http.Request request) {
        return ok(views.html.login.render(loginForm, request, messagesApi.preferred(request)));
    }

    public Result dashboard(Http.Request request) {
        return ok(views.html.dashboard.render());
    }

    public Result handleLogin(Http.Request request) {
        logger.info("Handling login request");

        Form<Login> boundForm = loginForm.bindFromRequest(request);

        if (boundForm.hasErrors()) {
            logger.error("Form has errors: " + boundForm.errorsAsJson());
            return badRequest(views.html.login.render(boundForm, request, messagesApi.preferred(request)));
        }

        Login login = boundForm.get();
        logger.info("Login attempt for email: " + login.getEmail());

        User user = User.find.query().where().eq("email", login.getEmail()).findOne();

        if (user == null) {
            logger.error("User not found for email: " + login.getEmail());
            boundForm = boundForm.withError(new ValidationError("email", "Invalid email or password"));
            return badRequest(views.html.login.render(boundForm, request, messagesApi.preferred(request)));
        }

        if (!user.checkPassword(login.getPassword())) {
            logger.error("Invalid password for email: " + login.getEmail());
            boundForm = boundForm.withError(new ValidationError("email", "Invalid email or password"));
            return badRequest(views.html.login.render(boundForm, request, messagesApi.preferred(request)));
        }

        logger.info("Login successful for email: " + login.getEmail());
        return redirect(routes.HomeController.dashboard()).flashing("success", "Logged in successfully");
    }

    public Result register(Http.Request request) {
        return ok(views.html.register.render(userForm, request, messagesApi.preferred(request)));
    }

    public Result handleRegister(Http.Request request) {
        Form<User> boundForm = userForm.bindFromRequest(request);
        String confirmPassword = formFactory.form().bindFromRequest(request).get("confirmPassword");

        if (boundForm.hasErrors()) {
            return badRequest(views.html.register.render(boundForm, request, messagesApi.preferred(request)));
        }

        User user = boundForm.get();

        if (!user.getPassword().equals(confirmPassword)) {
            boundForm = boundForm.withError(new ValidationError("confirmPassword", "Passwords do not match"));
            return badRequest(views.html.register.render(boundForm, request, messagesApi.preferred(request)));
        }

        // Check for existing email
        User existingUser = User.find.query().where().eq("email", user.getEmail()).findOne();
        if (existingUser != null) {
            boundForm = boundForm.withError(new ValidationError("email", "Email is already taken"));
            return badRequest(views.html.register.render(boundForm, request, messagesApi.preferred(request)));
        }

        user.setPassword(user.getPassword());
        user.save();

        return redirect(routes.HomeController.login()).flashing("success", "User successfully registered");
    }
}
