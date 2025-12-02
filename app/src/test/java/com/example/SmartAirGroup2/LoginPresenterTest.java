package com.example.SmartAirGroup2;

import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;
import com.example.SmartAirGroup2.auth.login.LoginContract;
import com.example.SmartAirGroup2.auth.login.LoginPresenter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

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

    private LoginPresenter presenter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        presenter = new LoginPresenter(mockAuthRepository);
        presenter.attach(mockView);
    }

    @Test
    public void testLogin_emptyEmail(){
        String email = "";
        presenter.onLoginClicked("parent", "perry", email, "!Abc12345");
        verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_nullEmail(){
        String email = null;
        presenter.onLoginClicked("parent", "perry", email, "!Abc12345");
        verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_emptyUname(){
        String Uname = "";
        presenter.onLoginClicked("parent",Uname , "perry@gmail.com", "!Abc12345");
        verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_nullUname(){
        String Uname = null;
        presenter.onLoginClicked("parent", Uname, "perry@gmail.com", "!Abc12345");
        verify(mockView).showLoginFailed();
    }  @Test

    public void testLogin_emptyPassward(){
        String passward = "";
        presenter.onLoginClicked("parent", "perry", "perry@gmail.com", passward);
        verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_nullPassward(){
        String passward = null;
        presenter.onLoginClicked("parent", "perry", "perry@gmail.com", passward);
        verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_nullView_emptyPassword(){
        presenter.detach();
        presenter.onLoginClicked("parent", "perry", "perry@gmail.com", "");
        verifyNoInteractions(mockView);
        verifyNoInteractions(mockAuthRepository);
    }

    @Test
    public void testLogin_nullView () throws Exception{
        when(mockAuthRepository.CheckPassword("parent","perry","perry@gmail.com","!Abc12345"))
                .thenReturn(true);

        presenter.detach();

        presenter.onLoginClicked("parent","perry","perry@gmail.com","!Abc12345");
        Thread.sleep(50);

        verify(mockAuthRepository).CheckPassword(anyString(), anyString(), anyString(), anyString());
        verifyNoInteractions(mockView);
    }

    @Test
    public void testLogin_loginSuccess() throws Exception{
        when(mockAuthRepository.CheckPassword("parent","perry","perry@gmail.com","!Abc12345"
        )).thenReturn(true);

        presenter.onLoginClicked("parent","perry","perry@gmail.com","!Abc12345");
        Thread.sleep(50);

        verify(mockView).showLoginSuccess("parent");
        verify(mockView, never()).showLoginFailed();
    }
    @Test
    public void testLogin_invalidCredentials() throws Exception{
    when(mockAuthRepository.CheckPassword(
            anyString(), anyString(), anyString(), anyString())
    ).thenReturn(false);

    presenter.onLoginClicked("parent", "perry", "perry@gmail.com", "wrongPass");

    verify(mockAuthRepository).CheckPassword("parent", "perry", "perry@gmail.com", "wrongPass");
    verify(mockView).showLoginFailed();
    }

    @Test
    public void testLogin_throwException_nullView() throws Exception{
        when(mockAuthRepository.CheckPassword(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));
        presenter.detach();
        presenter.onLoginClicked("child","perry","perry@gmail.com","!Abc12345");
        Thread.sleep(50);

        verifyNoInteractions(mockView);
    }

    @Test
    public void testLogin_throwException() throws Exception{
        when(mockAuthRepository.CheckPassword(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("failed"));
        presenter.onLoginClicked("child","perry","perry@gmail.com","!Abc12345");
        Thread.sleep(50);

        verify(mockView).showInputError("failed");
        verify(mockView).showLoginFailed();
    }

}