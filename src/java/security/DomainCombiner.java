package java.security;

/**
 * @className: DomainCombiner
 * @author: doudou
 * @datetime: 2021/11/4
 * @description: {@code DomainCombiner} 提供了一种动态更新与当前 {@code AccessControlContext} 关联的 ProtectionDomains 的方法。
 */
public interface DomainCombiner {

    /**
     * 修改或更新提供的 ProtectionDomains。 ProtectionDomain 可以添加到给定的 ProtectionDomain 或从中删除。
     * ProtectionDomain 可以重新排序。 可以修改单个保护域（例如，使用一组新的权限）。
     */
    ProtectionDomain[] combine(ProtectionDomain[] currentDomains, ProtectionDomain[] assignedDomains);
}
