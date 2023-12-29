package metaint.replanet.rest.org.controller;


import io.swagger.annotations.ApiOperation;
import metaint.replanet.rest.org.dto.OrgRequestDTO;
import metaint.replanet.rest.org.entity.Organization;
import metaint.replanet.rest.org.service.OrgService;
import metaint.replanet.rest.pay.entity.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


import lombok.extern.slf4j.Slf4j;
import metaint.replanet.rest.privacy.dto.MemberDTO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@CrossOrigin("http://localhost:3000/")
@RestController
public class OrgController {

    private final OrgService orgService;

    public OrgController (OrgService orgService){
        this.orgService = orgService;
    }

    @ApiOperation(value = "기부처 정보 전체 조회 요청", notes = "등록된 기부처를 전체 조회합니다.", tags = {"기부처 전체 조회"})
    @GetMapping("/orgs")
    public ResponseEntity<List<Map<String, Object>>> getOrgs() {
        // 유효성 체크한다고 하면 현재 로그인한 놈의 ROLE_ADMIN인지 확인하는 정도 
        List<Map<String, Object>> orgList = orgService.getOrgList();

        return new ResponseEntity<>(orgList, HttpStatus.OK);

    }

    @ApiOperation(value = "기부처 정보 상세 조회", notes = "등록된 기부처의 비밀번호를 검증합니다.", tags = {"기부처 상세 조회"})
    @GetMapping("orgInfo/{memberCode}")
    public ResponseEntity<?> selectOrgInformation(@PathVariable int memberCode,
                                                  @RequestParam String orgPwd){

        int verify = orgService.verifyPassword(memberCode, orgPwd);
        if(verify == 1){
            List<Map<String, Object>> orgInformation = orgService.selectOrgInformation(memberCode);
            return ResponseEntity.status(HttpStatus.OK).body(orgInformation);
        } else if(verify == 0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호가 일치하지 않습니다.");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("비밀번호를 검증하지 못했습니다.");
    }

    @ApiOperation(value = "기부처 정보 수정 요청", notes = "등록된 기부처의 정보를 수정합니다.", tags = {"기부처 정보 수정"})
    @PostMapping("orgModify/{memberCode}")
    public ResponseEntity<?> updateOrgInformation(@PathVariable int memberCode, @RequestPart(value = "file", required = false)MultipartFile orgImg,
                                                  @RequestPart(value = "orgDescription") String orgDescription, @RequestPart(value = "password") String password,
                                                  @RequestPart(value = "memberName") String memberName, @RequestPart(value = "phone") String phone){

        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setPassword(password);
        memberDTO.setMemberName(memberName.substring(1, memberName.length()-1));
        memberDTO.setPhone(phone);
        memberDTO.setMemberCode(memberCode);

        if(orgImg != null){
            try{
                String fileOriginName = orgImg.getOriginalFilename();
                String fileExtension = fileOriginName.substring(fileOriginName.lastIndexOf("."));
                String fileSaveName = UUID.randomUUID().toString().replaceAll("-", "") + fileExtension;
                String FILE_DIR = null;
                Path rootPath;
                if (FileSystems.getDefault().getSeparator().equals("/")) {
                    // Unix-like system (MacOS, Linux)
                    Path filePath1 = Paths.get("/REPLANET_React_V2/public/orgImgs/" + memberCode).toAbsolutePath();
                    rootPath = Paths.get("/User").toAbsolutePath();
                    Path relativePath = rootPath.relativize(filePath1);
                    FILE_DIR = String.valueOf(relativePath);
                } else {
                    // Windows
                    Path filePath2 = Paths.get("/dev/metaint/REPLANET_React_V2/public/orgImgs/" + memberCode).toAbsolutePath();
                    rootPath = Paths.get("C:\\").toAbsolutePath();
                    Path relativePath = rootPath.resolve(filePath2);
                    FILE_DIR = String.valueOf(relativePath);
                }
                OrgRequestDTO orgUpdate = new OrgRequestDTO();
                orgUpdate.setFileOriginName(fileOriginName);
                orgUpdate.setFileSaveName(fileSaveName);
                orgUpdate.setFileSavePath(FILE_DIR);
                orgUpdate.setFileExtension(fileExtension);
                orgUpdate.setOrgCode(memberCode);
                orgUpdate.setOrgDescription(orgDescription.substring(1, orgDescription.length()-1));

                try {
                    File directory = new File(FILE_DIR);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    File pf = new File(FILE_DIR + "/" + fileSaveName);
                    orgImg.transferTo(pf);

                    orgService.updateOrgInfo(orgUpdate);
                    orgService.updateMemberInfo(memberDTO);

                    return ResponseEntity.status(HttpStatus.OK).body("수정 완료");
                } catch (IOException e){
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 중 오류");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 중 오류");
            }
        } else {
            //이미지 변경 없을 때
            try{
                OrgRequestDTO orgUpdate = new OrgRequestDTO();
                orgUpdate.setOrgCode(memberCode);
                orgUpdate.setOrgDescription(orgDescription.substring(1, orgDescription.length()-1));

                orgService.updateOrgInfo(orgUpdate);
                orgService.updateMemberInfo(memberDTO);

                return ResponseEntity.status(HttpStatus.OK).body("수정 완료");
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 중 오류");
            }
        }
    }

    @ApiOperation(value = "기부처 탈퇴 처리", notes = "기부처 정보 삭제", tags = {"기부처 정보 삭제"})
    @PutMapping("/withdrawOrg")
    public void putWithdrawOrg(@RequestBody MemberDTO memberDTO, HttpServletResponse response){

        Long memberCode = (long) memberDTO.getMemberCode();

        if (memberCode > 0) {
            int result = orgService.deleteOrgByMemberCode(memberCode);

            if (result > 0) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @ApiOperation(value = "기부처 탈퇴 요청", notes = "로그인한 기부처의 비밀번호와 진행중인 캠페인이 없는지 검증한 후, 등록된 기부처 정보를 삭제 요청합니다.", tags = {"기부처 정보 삭제 요청"})
    @PutMapping("orgWithdraw/{memberCode}")
    public ResponseEntity<?> updateOrgWithdraw(@PathVariable int memberCode, @RequestParam(value = "enterReason") String withdrawReason, @RequestParam(value = "password") String password){

        OrgRequestDTO newRequest = new OrgRequestDTO();
        newRequest.setOrgCode(memberCode);
        newRequest.setWithdrawReqDate(new Date());
        newRequest.setWithdrawReason(withdrawReason);

        int verify = orgService.verifyPassword(memberCode, password);

        int check = orgService.checkCampaign(memberCode);

        if(check > 0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("campaigns");
        } else {
            if(verify == 1){
                try{
                    orgService.updateOrgWithdraw(newRequest);
                    return ResponseEntity.status(HttpStatus.OK).body("탈퇴 신청 완료");
                } catch (Exception e){
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("업데이트 처리 중 오류");
                }
            } else if(verify == 0){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("WrongPwd");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호 검증 혹은 진행중인 캠페인 조회 오류.");
    }
}
