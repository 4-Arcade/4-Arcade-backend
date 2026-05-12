package com.fourarcade.arcadebackend.mypage;

import com.fourarcade.arcadebackend.common.exception.BusinessException;
import com.fourarcade.arcadebackend.quiz.Quiz;
import com.fourarcade.arcadebackend.quiz.QuizRepository;
import com.fourarcade.arcadebackend.user.User;
import com.fourarcade.arcadebackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MyQuizListResponse getMyQuizzes(UUID userId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (size < 1) {
            throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다.");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<Quiz> quizPage = quizRepository.findByUser_Id(userId, pageable);

        return MyQuizListResponse.from(quizPage);
    }

    @Transactional
    public MyProfileResponse updateNickname(UUID userId, NicknameUpdateRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(
                        "USER_NOT_FOUND",
                        "유저를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        user.updateNickname(request.getNickname());

        return MyProfileResponse.from(user);
    }
}