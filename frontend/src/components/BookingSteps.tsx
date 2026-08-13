const steps = ['Phim', 'Suất chiếu', 'Ghế', 'Xác nhận', 'Thanh toán'];

interface BookingStepsProps {
  active: number;
}

export function BookingSteps({ active }: BookingStepsProps) {
  return (
    <div className="booking-steps" aria-label="Tiến trình đặt vé">
      {steps.map((step, index) => {
        const state = index < active ? 'done' : index === active ? 'active' : '';
        return (
          <div
            key={step}
            className={`booking-step ${state}`}
            aria-current={index === active ? 'step' : undefined}
          >
            <div className="step-track">
              <span>{index + 1}</span>
            </div>
            <b>{step}</b>
          </div>
        );
      })}
    </div>
  );
}
