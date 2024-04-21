package codesquad.springcafe.controller;

import codesquad.springcafe.database.comment.CommentDatabase;
import codesquad.springcafe.form.article.ArticleWriteForm;
import codesquad.springcafe.form.comment.CommentWriteForm;
import codesquad.springcafe.form.user.LoginUser;
import codesquad.springcafe.model.Article;
import codesquad.springcafe.model.Comment;
import codesquad.springcafe.service.ArticleService;
import codesquad.springcafe.util.LoginUserProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequestMapping("/articles")
public class ArticleController {
    private final Logger logger = LoggerFactory.getLogger(ArticleController.class);

    private final ArticleService articleService;
    private final CommentDatabase commentDatabase;

    public ArticleController(ArticleService articleService, CommentDatabase commentDatabase) {
        this.articleService = articleService;
        this.commentDatabase = commentDatabase;
    }

    /**
     * 사용자에게 아티클 폼을 보여줍니다.
     */
    @GetMapping("/add")
    public String articleForm(@ModelAttribute("articleWriteForm") ArticleWriteForm articleWriteForm) {
        return "article/form";
    }

    /**
     * 사용자가 작성한 아티클을 생성하고 데이터베이스에 저장합니다.
     */
    @PostMapping("/add")
    public String addArticle(@Validated @ModelAttribute ArticleWriteForm articleWriteForm, BindingResult bindingResult,
                             @SessionAttribute(LoginController.LOGIN_SESSION_NAME) LoginUser loginUser) {
        if (bindingResult.hasErrors()) {
            logger.error("errors ={}", bindingResult);
            return "article/form";
        }

        Article article = articleService.write(articleWriteForm, loginUser.getNickname()); // 게시글 업데이트
        loginUser.addOwnArticle(article.getId()); // 세션정보 업데이트

        return "redirect:/";
    }

    /**
     * 사용자가 요청한 id의 아티클을 조회수를 올리고 렌더링하여 보여줍니다. 일치하는 id가 데이터베이스에 존재하지 않는다면 홈으로 리다이렉트합니다.
     */
    @GetMapping("/detail/{id}")
    public String viewArticle(@PathVariable Long id, Model model,
                              @ModelAttribute("commentWriteForm") CommentWriteForm commentWriteForm) {

        Article article = articleService.viewArticle(id);
        List<Comment> comments = commentDatabase.findAll(id);
        model.addAttribute("article", article);
        model.addAttribute("comments", comments);

        return "article/show";
    }

    /**
     * id와 일치하는 게시글을 찾고 수정 폼을 보여줍니다.
     */
    @GetMapping("/edit/{id}")
    public String updateForm(@PathVariable Long id, Model model) {
        ArticleWriteForm articleUpdateForm = articleService.getArticleUpdateForm(id);
        model.addAttribute("articleWriteForm", articleUpdateForm);
        return "article/update";
    }

    /**
     * id와 일치하는 게시글을 찾고 업데이트합니다.
     */
    @PutMapping("/edit/{id}")
    public String updateArticle(@PathVariable Long id, @Validated @ModelAttribute ArticleWriteForm articleWriteForm,
                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "article/update";
        }
        articleService.update(id, articleWriteForm);

        return "redirect:/articles/detail/" + id;
    }

    /**
     * id와 일치하는 게시글을 찾아 삭제 폼을 보여줍니다.
     */
    @GetMapping("/delete/{id}")
    public String deleteForm(@PathVariable Long id, Model model) {
        model.addAttribute("articleId", id);
        return "article/delete";
    }

    /**
     * 게시물의 작성자와 다른 유저가 작성한 코멘트가 존재할 경우 게시물을 삭제할 수 없습니다.
     */
    @DeleteMapping("/delete/{id}")
    public String deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
        deleteComments(id);
//        TODO: 커멘트 서비스로 추출해야함

        return "redirect:/";
    }

    /**
     * 코멘트를 작성합니다. 게시글이 존재하지 않으면 홈으로 리다이렉트하고 댓글 폼에서 에러가 발생하면 게시물 상세 조회 페이지로 리다이렉트합니다.
     */
    @PostMapping("/detail/{articleId}/comments")
    public String writeComment(@PathVariable Long articleId,
                               @Validated @ModelAttribute CommentWriteForm commentWriteForm,
                               BindingResult bindingResult, Model model,
                               HttpSession session) {
        Article article = articleService.getArticle(articleId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("article", article);
            model.addAttribute("comments", commentDatabase.findAll(articleId));
            return "article/show";
        }
        LoginUser loginUser = LoginUserProvider.provide(session);
        Comment comment = new Comment(loginUser.getNickname(), commentWriteForm.getContent(), articleId,
                LocalDateTime.now());

        commentDatabase.add(comment);
        logger.info("새로운 코멘트가 추가되었습니다. {}", comment);

        return "redirect:/articles/detail/" + articleId;
    }

    /**
     * 코멘트를 삭제합니다. 아티클 id나 코멘트 id가 유효하지 않으면 홈으로 이동합니다. 로그인 유저가 아닌 사용자가 요청을 보낼 경우 403 응답을 내보냅니다.
     */
    @DeleteMapping("/detail/{articleId}/comments/{id}")
    public String deleteComment(@PathVariable Long articleId, @PathVariable Long id, HttpSession session,
                                HttpServletResponse response) throws IOException {
        Article article = articleService.getArticle(articleId);
        Optional<Comment> optionalComment = commentDatabase.findBy(id);
        if (optionalComment.isEmpty()) {
            return "redirect:/";
        }

        Comment comment = optionalComment.get();
        LoginUser loginUser = LoginUserProvider.provide(session);
        if (!loginUser.hasSameNickname(comment.getWriter())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        comment.delete();
        commentDatabase.update(comment);
        logger.info("코멘트가 삭제되었습니다. {}", comment);
        return "redirect:/articles/detail/" + articleId;
    }

    private void deleteComments(Long id) {
        commentDatabase.findAll(id)
                .stream()
                .peek(Comment::delete)
                .forEach(commentDatabase::update);
    }
}
