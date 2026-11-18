package wcg.yhm.ymmknowledgeexchange.auth.rpc;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import wcg.yhm.user.api.controller.UserFeignApi;
import wcg.yhm.user.api.domain.dto.GetUserByPhoneRequest;
import wcg.yhm.user.api.domain.dto.UserRegisterRequest;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRpcServiceTest {

    @Test
    void passwordRegistrationPassesEncodedPassword() {
        UserFeignApi feignApi = mock(UserFeignApi.class);
        UserRpcService service = new UserRpcService();
        ReflectionTestUtils.setField(service, "userFeignApi", feignApi);
        when(feignApi.register(any(UserRegisterRequest.class))).thenReturn(Response.success(42L));

        assertEquals(42L, service.register("13800000000", "encoded-password"));
        org.mockito.ArgumentCaptor<UserRegisterRequest> request =
                org.mockito.ArgumentCaptor.forClass(UserRegisterRequest.class);
        verify(feignApi).register(request.capture());
        assertEquals("13800000000", request.getValue().getPhone());
        assertEquals("encoded-password", request.getValue().getEncodedPassword());
    }

    @Test
    void onlySuccessfulEmptyLookupMeansUserDoesNotExist() {
        UserFeignApi feignApi = mock(UserFeignApi.class);
        UserRpcService service = new UserRpcService();
        ReflectionTestUtils.setField(service, "userFeignApi", feignApi);
        when(feignApi.getByPhone(any(GetUserByPhoneRequest.class))).thenReturn(Response.success());

        assertNull(service.getByPhone("13800000000"));

        when(feignApi.getByPhone(any(GetUserByPhoneRequest.class)))
                .thenReturn(Response.fail("服务不可用"));
        assertThrows(BizException.class, () -> service.getByPhone("13800000000"));
    }
}
