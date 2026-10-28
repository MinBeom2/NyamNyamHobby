import { zodResolver } from '@hookform/resolvers/zod';
import { MessageCircleQuestion } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { inquiryCategoryLabels, useSupportMockStore, type InquiryCategory } from '../../features/support/model/supportMockStore';
import { characterAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';

const inquirySchema = z.object({
  category: z.enum(['PLAN', 'EXPLORATION', 'HOBBY', 'ACCOUNT', 'ETC']),
  title: z.string().trim().min(1, '제목을 입력해 주세요.').max(50, '제목은 50자 이하로 입력해 주세요.'),
  content: z.string().trim().min(1, '문의 내용을 입력해 주세요.').max(2000, '내용은 2,000자 이하로 입력해 주세요.'),
});

type InquiryFormValues = z.infer<typeof inquirySchema>;

function SupportHeader() {
  const unreadCount = useSupportMockStore((state) => state.notifications.filter((item) => !item.read).length);
  return <AppHeader authenticated notificationCount={unreadCount} />;
}

export function InquiryListPage() {
  const inquiries = useSupportMockStore((state) => state.inquiries);
  return <div className="site-shell"><SupportHeader /><main className="page-container support-page">
    <header className="support-heading"><h1>고객 문의</h1><Link className="button button--primary" to="/support/inquiries/new">문의하기</Link></header>
    {inquiries.length === 0 ? <InquiryEmptyState /> : <div className="inquiry-list">
      {inquiries.map((item) => <Link key={item.id} className="inquiry-list__item" to={`/support/inquiries/${item.id}`}>
        <div><span>{inquiryCategoryLabels[item.category]}</span><strong>{item.title}</strong></div>
        <div><b className={item.status === 'ANSWERED' ? 'answered' : ''}>{item.status === 'ANSWERED' ? '답변 완료' : '답변 대기'}</b><time>{item.createdAt}</time><i>›</i></div>
      </Link>)}
    </div>}
  </main></div>;
}

function InquiryEmptyState() {
  return <section className="inquiry-empty"><img src={characterAssets.apology} alt="등록된 문의가 없어 아쉬워하는 냠냠취미 캐릭터" /><h2>현재 등록된 문의가 없어요</h2><p>궁금하거나 불편한 점이 있다면 문의를 남겨 주세요.</p><Link className="button button--primary" to="/support/inquiries/new">문의하기</Link></section>;
}

export function InquiryDetailPage() {
  const { inquiryId } = useParams();
  const inquiry = useSupportMockStore((state) => state.inquiries.find((item) => item.id === Number(inquiryId)));
  if (!inquiry) return <Navigate replace to="/support/inquiries" />;
  return <div className="site-shell"><SupportHeader /><main className="page-container support-page">
    <header className="support-heading"><h1>고객 문의</h1></header>
    <article className="inquiry-detail">
      <div className="inquiry-detail__badges"><span>{inquiryCategoryLabels[inquiry.category]}</span><b className={inquiry.status === 'ANSWERED' ? 'answered' : ''}>{inquiry.status === 'ANSWERED' ? '답변 완료' : '답변 대기'}</b></div>
      <h2>{inquiry.title}</h2><time>{inquiry.createdAt}</time><p className="inquiry-detail__question">{inquiry.content}</p>
      {inquiry.answer ? <section className="inquiry-answer"><div><strong>관리자 답변</strong><time>{inquiry.answer.answeredAt}</time></div><p>{inquiry.answer.content}</p></section> : <section className="inquiry-answer inquiry-answer--pending"><MessageCircleQuestion /><div><strong>답변을 준비하고 있어요</strong><p>확인 후 알림으로 알려드릴게요.</p></div></section>}
      <Link className="button button--secondary" to="/support/inquiries">목록으로</Link>
    </article>
  </main></div>;
}

export function InquiryWritePage() {
  const navigate = useNavigate();
  const addInquiry = useSupportMockStore((state) => state.addInquiry);
  const { register, handleSubmit, formState: { errors } } = useForm<InquiryFormValues>({ resolver: zodResolver(inquirySchema), defaultValues: { category: 'EXPLORATION', title: '', content: '' } });
  const submit = handleSubmit((values) => navigate(`/support/inquiries/${addInquiry(values)}`, { replace: true }));
  return <div className="site-shell"><SupportHeader /><main className="page-container inquiry-write-page"><section aria-labelledby="inquiry-write-title"><h1 id="inquiry-write-title">문의하기</h1>
    <form className="inquiry-form" onSubmit={submit} noValidate>
      <label><strong>문의 유형</strong><select {...register('category')}>{(Object.entries(inquiryCategoryLabels) as [InquiryCategory,string][]).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label><strong>제목</strong><input type="text" maxLength={50} placeholder="제목을 입력하세요 (50자)" {...register('title')} />{errors.title && <small>{errors.title.message}</small>}</label>
      <label><strong>내용</strong><textarea maxLength={2000} placeholder="문의 내용을 자세히 적어 주세요 (2,000자)" {...register('content')} />{errors.content && <small>{errors.content.message}</small>}</label>
      <label className="inquiry-file"><span>이미지 첨부 (1장)</span><input type="file" accept="image/jpeg,image/png" /><small>JPG · PNG · 5MB 이하</small></label>
      <div><button className="button button--secondary" type="button" onClick={() => navigate('/support/inquiries')}>취소</button><button className="button button--primary" type="submit">등록</button></div>
    </form>
  </section></main></div>;
}
