package com.example.SmartAirGroup2;

import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;
import com.example.SmartAirGroup2.auth.login.LoginContract;
import com.example.SmartAirGroup2.auth.login.LoginPresenter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import android.os.Handler;
import android.os.Looper;

/**
 * Unit tests for LoginPresenter
 * Tests the business logic of login validation and authentication
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginPresenterTest {

    @Mock
    private LoginContract.View mockView;

    @Mock
    private AuthRepository mockAuthRepository;

    @Mock
    private Handler mockHandler;

    private LoginPresenter presenter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return true;
        }).when(mockHandler).post(any(Runnable.class));

        presenter = new LoginPresenter(mockAuthRepository, mockHandler);
        presenter.attach(mockView);
    }

    @Test
    public void testLogin_emptyEmail(){
        String email = "";
        presenter.onLoginClicked("parent", "perry", email, "!Abc12345");
        Mockito.verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_nullEmail(){
        String email = null;
        presenter.onLoginClicked("parent", "perry", email, "!Abc12345");
        Mockito.verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_emptyUname(){
        String Uname = "";
        presenter.onLoginClicked("parent",Uname , "perry@gmail.com", "!Abc12345");
        Mockito.verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_nullUname(){
        String Uname = null;
        presenter.onLoginClicked("parent", Uname, "perry@gmail.com", "!Abc12345");
        Mockito.verify(mockView).showLoginFailed();
    }  @Test

    public void testLogin_emptyPassward(){
        String passward = "";
        presenter.onLoginClicked("parent", "perry", "perry@gmail.com", passward);
        Mockito.verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_nullPassward(){
        String passward = null;
        presenter.onLoginClicked("parent", "perry", "perry@gmail.com", passward);
        Mockito.verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_nullView_emptyPassword(){
        presenter.detach();
        presenter.onLoginClicked("parent", "perry", "perry@gmail.com", "");
        Mockito.verifyNoInteractions(mockView);
        Mockito.verifyNoInteractions(mockAuthRepository);
    }

    @Test
    public void testLogin_nullView () throws Exception{
        Mockito.when(mockAuthRepository.CheckPassword("parent","perry","perry@gmail.com","!Abc12345"))
                .thenReturn(true);

        presenter.detach();

        presenter.onLoginClicked("parent","perry","perry@gmail.com","!Abc12345");
        Thread.sleep(50);

        Mockito.verify(mockAuthRepository).CheckPassword(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.verifyNoInteractions(mockView);
    }

    @Test
    public void testLogin_loginSuccess() throws Exception{
        Mockito.when(mockAuthRepository.CheckPassword("parent","perry","perry@gmail.com","!Abc12345"
        )).thenReturn(true);

        presenter.onLoginClicked("parent","perry","perry@gmail.com","!Abc12345");
        Thread.sleep(50);

        Mockito.verify(mockView).showLoginSuccess("parent");
        Mockito.verify(mockView, Mockito.never()).showLoginFailed();
    }

    @Test
    public void testLogin_invalidCredentials() throws Exception {
        when(mockAuthRepository.CheckPassword(
                anyString(), anyString(), anyString(), anyString())
        ).thenReturn(false);

        presenter.onLoginClicked("parent","perry","perry@gmail.com","wrongPass");

        Thread.sleep(50);

        verify(mockView).showLoginFailed();
    }


    @Test
    public void testLogin_throwException_nullView() throws Exception{
        Mockito.when(mockAuthRepository.CheckPassword(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));
        presenter.detach();
        presenter.onLoginClicked("child","perry","perry@gmail.com","!Abc12345");
        Thread.sleep(50);

        Mockito.verifyNoInteractions(mockView);
    }

    @Test
    public void testLogin_throwException() throws Exception{
        Mockito.when(mockAuthRepository.CheckPassword(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("failed"));
        presenter.onLoginClicked("child","perry","perry@gmail.com","!Abc12345");
        Thread.sleep(50);

        Mockito.verify(mockView).showInputError("failed");
        Mockito.verify(mockView).showLoginFailed();
    }

}