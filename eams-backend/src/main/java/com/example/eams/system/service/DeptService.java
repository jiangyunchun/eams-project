package com.example.eams.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.system.dto.DeptDTO;
import com.example.eams.system.entity.SysDept;
import com.example.eams.system.entity.SysUser;
import com.example.eams.system.mapper.SysDeptMapper;
import com.example.eams.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理服务
 */
@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    /**
     * 查询部门树（全量，前端处理树形）
     */
    public List<SysDept> listAll() {
        return deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getIsDeleted, 0)
                        .orderByAsc(SysDept::getSortOrder));
    }

    /**
     * 查询单个部门
     */
    public SysDept getDetail(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) throw BusinessException.notFound("部门不存在");
        return dept;
    }

    /**
     * 新增部门
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(DeptDTO dto) {
        // 同级重名校验
        checkNameUnique(dto.getParentId(), dto.getDeptName(), null);
        // 编码唯一校验
        checkCodeUnique(dto.getDeptCode(), null);

        SysDept dept = new SysDept();
        dept.setParentId(dto.getParentId() != null ? dto.getParentId() : 0);
        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setLeaderId(dto.getLeaderId());
        dept.setSortOrder(dto.getSortOrder());
        dept.setStatus(dto.getStatus());
        deptMapper.insert(dept);
    }

    /**
     * 编辑部门
     */
    @Transactional(rollbackFor = Exception.class)
    public void edit(DeptDTO dto) {
        SysDept dept = deptMapper.selectById(dto.getId());
        if (dept == null) throw BusinessException.notFound("部门不存在");

        checkNameUnique(dto.getParentId(), dto.getDeptName(), dto.getId());
        checkCodeUnique(dto.getDeptCode(), dto.getId());

        dept.setParentId(dto.getParentId() != null ? dto.getParentId() : 0);
        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setLeaderId(dto.getLeaderId());
        dept.setSortOrder(dto.getSortOrder());
        dept.setStatus(dto.getStatus());
        deptMapper.updateById(dept);
    }

    /**
     * 删除部门
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) throw BusinessException.notFound("部门不存在");

        // 检查是否有子部门
        Long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getParentId, id)
                        .eq(SysDept::getIsDeleted, 0));
        if (childCount > 0) {
            throw new BusinessException(400, "该部门下存在子部门，请先删除子部门");
        }

        // 检查是否有用户
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeptId, id)
                        .eq(SysUser::getIsDeleted, 0));
        if (userCount > 0) {
            throw new BusinessException(400,
                    "该部门下存在" + userCount + "个用户，请先将用户转移至其他部门");
        }

        deptMapper.deleteById(id);
    }

    /**
     * 获取部门的完整路径名称（用于前端展示）
     */
    public String getDeptPathName(Long deptId) {
        if (deptId == null) return "";
        StringBuilder path = new StringBuilder();
        buildPath(deptId, path);
        return path.toString();
    }

    // ==================== 内部方法 ====================

    private void buildPath(Long deptId, StringBuilder path) {
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null) return;
        if (dept.getParentId() != null && dept.getParentId() != 0) {
            buildPath(dept.getParentId(), path);
            path.append(" / ");
        }
        path.append(dept.getDeptName());
    }

    private void checkNameUnique(Long parentId, String deptName, Long excludeId) {
        LambdaQueryWrapper<SysDept> qw = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parentId != null ? parentId : 0)
                .eq(SysDept::getDeptName, deptName)
                .eq(SysDept::getIsDeleted, 0);
        if (excludeId != null) qw.ne(SysDept::getId, excludeId);
        if (deptMapper.selectCount(qw) > 0) {
            throw new BusinessException(400, "同级部门名称已存在");
        }
    }

    private void checkCodeUnique(String deptCode, Long excludeId) {
        LambdaQueryWrapper<SysDept> qw = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptCode, deptCode)
                .eq(SysDept::getIsDeleted, 0);
        if (excludeId != null) qw.ne(SysDept::getId, excludeId);
        if (deptMapper.selectCount(qw) > 0) {
            throw new BusinessException(400, "部门编码已存在");
        }
    }
}
