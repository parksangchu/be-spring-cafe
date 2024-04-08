package codesquad.springcafe.controller;

import codesquad.springcafe.database.UserDatabase;
import codesquad.springcafe.model.User;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserController {
    private final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserDatabase userDatabase;

    public UserController(UserDatabase userDatabase) {
        this.userDatabase = userDatabase;
    }

    @GetMapping
    public String userList(Model model) {
        List<User> users = userDatabase.findAll();
        model.addAttribute("users", users);
        return "/user/list";
    }


    @GetMapping("/add")
    public String userForm() {
        return "/user/form";
    }

    @PostMapping("/add")
    public String addUser(@ModelAttribute User user) {
        userDatabase.save(user);
        logger.info("새로운 유저가 생성되었습니다. {}", user);
        return "redirect:/users";
    }

    @PostConstruct
    public void createTestUser() {
        User user = new User("sangchu", "123", "상추", "sangchu@gmail.com");
        userDatabase.save(user);
    }
}
